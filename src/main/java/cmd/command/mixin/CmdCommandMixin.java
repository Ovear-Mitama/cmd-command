package cmd.command.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.*;
import java.lang.reflect.Field;

@Mixin(ClientPacketListener.class)
public class CmdCommandMixin {

	private static final TextColor GREEN = TextColor.fromRgb(0xB1EAC2);
	private static final TextColor RED = TextColor.fromRgb(0xF9867D);
	private static final TextColor GRAY = TextColor.fromRgb(0xCCCCCC);
	private static final TextColor YELLOW = TextColor.fromRgb(0xFFD700);

	private static final TerminalManager TM = new TerminalManager();

	// ---- Translation helper ----

	private static String ts(String key, Object... args) {
		String fmt = net.minecraft.locale.Language.getInstance().getOrDefault(key);
		return args.length > 0 ? String.format(fmt, args) : fmt;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.literal(ts(key, args));
	}

	// ---- Brigadier: /cmd internal ... ----

	@Inject(method = "<init>", at = @At("TAIL"))
	private void onInit(CallbackInfo ci) {
		try {
			Field field = findDispatcherField(ClientPacketListener.class);
			if (field == null) return;
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			CommandDispatcher<CommandSourceStack> d =
				(CommandDispatcher<CommandSourceStack>) field.get(this);

			var cmd = Commands.literal("cmd");

			// --- /cmd internal ---
			var internal = Commands.literal("internal");
			internal.then(Commands.literal("start").executes(ctx -> { TM.start(); return 1; }));
			internal.then(Commands.literal("list").executes(ctx -> { TM.list(); return 1; }));
			internal.then(Commands.literal("help").executes(ctx -> { showHelp(ctx.getSource()); return 1; }));
			for (int i = 0; i < 10; i++) {
				final int n = i;
				internal.then(Commands.literal(String.valueOf(i)).executes(ctx -> {
					TM.switchTo(n); return 1;
				}));
			}
			internal.executes(ctx -> { showHelp(ctx.getSource()); return 0; });
			cmd.then(internal);

			// --- /cmd <command> ---
			cmd.then(Commands.argument("command", StringArgumentType.greedyString())
				.executes(ctx -> {
					TM.execute(StringArgumentType.getString(ctx, "command"));
					return 1;
				}));

			// --- /cmd (no args) ---
			cmd.executes(ctx -> { showHelp(ctx.getSource()); return 0; });

			d.register(cmd);
		} catch (Exception ignored) {}
	}

	private static Field findDispatcherField(Class<?> clazz) {
		while (clazz != null && clazz != Object.class) {
			for (Field f : clazz.getDeclaredFields()) {
				if (f.getType() == CommandDispatcher.class) return f;
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	// ---- Network fallback ----

	@Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
	private void onSendCommand(String cmd, CallbackInfo ci) {
		String t = cmd.trim();
		if (t.equals("cmd") || t.startsWith("cmd ")) {
			ci.cancel();
			handleRaw(t.length() > 3 ? t.substring(4).trim() : "");
		}
	}

	private void handleRaw(String args) {
		if (args.isEmpty()) { showHelp(null); return; }
		if (args.startsWith("internal ")) {
			String sub = args.substring(9).trim();
			if (sub.equals("start")) TM.start();
			else if (sub.equals("list")) TM.list();
			else if (sub.equals("help")) showHelp(null);
			else if (sub.matches("\\d")) TM.switchTo(Integer.parseInt(sub));
			else showHelp(null);
		} else {
			TM.execute(args);
		}
	}

	// ---- Help ----

	private static void showHelp(CommandSourceStack src) {
		String[] keys = {"help.title", "help.cmd", "help.start", "help.list", "help.switch", "help.help"};
		for (String k : keys) {
			TextColor c = k.equals("help.title") ? YELLOW : GREEN;
			Component msg = tr("cmdmod." + k).setStyle(Style.EMPTY.withColor(c));
			if (src != null) src.sendSystemMessage(msg);
			else chat(msg);
		}
	}

	// ---- Terminal Manager ----

	static class TerminalManager {
		private final TerminalSession[] slots = new TerminalSession[10];
		private int current = -1;

		synchronized void execute(String command) {
			if (current == -1 || slots[current] == null || !slots[current].isAlive()) {
				start();
			}
			if (current == -1 || slots[current] == null) {
				chat(tr("cmdmod.term.no_active").setStyle(Style.EMPTY.withColor(RED)));
				return;
			}
			TerminalSession ts = slots[current];
			chat(clr("> " + command, GRAY));
			ts.sendCommand(command);
		}

		synchronized void start() {
			int slot = -1;
			for (int i = 0; i < 10; i++) {
				if (slots[i] == null || !slots[i].isAlive()) { slot = i; break; }
			}
			if (slot == -1) { chat(tr("cmdmod.term.max").setStyle(Style.EMPTY.withColor(RED))); return; }

			int id = slot;
			try {
				ProcessBuilder pb = new ProcessBuilder("cmd.exe");
				pb.redirectErrorStream(true);
				Process p = pb.start();
				TerminalSession ts = new TerminalSession(id, p);
				slots[id] = ts;
				current = id;
				ts.startReader();
				int count = 0;
				for (TerminalSession s : slots) if (s != null && s.isAlive()) count++;
				chat(tr("cmdmod.term.started", id, count).setStyle(Style.EMPTY.withColor(GREEN)));
		} catch (Exception e) {
			slots[id] = null;
			chat(tr("cmdmod.term.start_fail", e.getMessage()).setStyle(Style.EMPTY.withColor(RED)));
			}
		}

		synchronized void killCurrent() {
			if (current == -1 || slots[current] == null) {
				chat(tr("cmdmod.term.no_active").setStyle(Style.EMPTY.withColor(RED)));
				return;
			}
			int id = current;
			slots[id].kill();
			slots[id] = null;
			chat(tr("cmdmod.term.closed", id).setStyle(Style.EMPTY.withColor(YELLOW)));
			current = -1;
			for (int i = 0; i < 10; i++) {
				if (slots[i] != null && slots[i].isAlive()) { current = i; break; }
			}
			if (current >= 0) chat(tr("cmdmod.term.switched", current).setStyle(Style.EMPTY.withColor(GRAY)));
		}

		synchronized void switchTo(int id) {
			if (id < 0 || id > 9 || slots[id] == null || !slots[id].isAlive()) {
				chat(tr("cmdmod.term.not_found", id).setStyle(Style.EMPTY.withColor(RED)));
				return;
			}
			current = id;
			chat(tr("cmdmod.term.switched", id).setStyle(Style.EMPTY.withColor(GREEN)));
		}

		synchronized void list() {
			boolean any = false;
			for (int i = 0; i < 10; i++) {
				if (slots[i] != null && slots[i].isAlive()) {
					any = true;
					String s = ts("cmdmod.term.running");
					String m = (i == current) ? " " + ts("cmdmod.term.current") : "";
					TextColor c = (i == current) ? GREEN : GRAY;
					chat(clr("[" + i + "] " + s + m, c));
				}
			}
			if (!any) chat(tr("cmdmod.term.no_terminals").setStyle(Style.EMPTY.withColor(GRAY)));
		}
	}

	// ---- Terminal Session ----

	static class TerminalSession {
		final int id;
		final Process process;
		private final BufferedWriter writer;
		private volatile boolean alive = true;

		TerminalSession(int id, Process process) {
			this.id = id;
			this.process = process;
			this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		}

		void startReader() {
			Thread reader = new Thread(() -> {
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(process.getInputStream(), "GBK"))) {
					String line;
					while (alive && (line = br.readLine()) != null) {
						final String msg = line;
						Minecraft.getInstance().execute(() -> {
							if (Minecraft.getInstance().player != null)
								chat(clr(msg, GREEN));
						});
					}
				} catch (IOException ignored) {}
				alive = false;
			}, "CmdMod-Term-" + id);
			reader.setDaemon(true);
			reader.start();
		}

		synchronized void sendCommand(String command) {
			if (!alive) return;
			try {
				writer.write(command);
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				alive = false;
				chat(tr("cmdmod.term.write_fail", id, e.getMessage()).setStyle(Style.EMPTY.withColor(RED)));
			}
		}

		void kill() {
			alive = false;
			try { writer.close(); } catch (IOException ignored) {}
			process.destroyForcibly();
		}

		boolean isAlive() { return alive && process.isAlive(); }
	}

	// ---- Helpers ----

	private static void chat(Component msg) {
		Minecraft.getInstance().execute(() -> {
			if (Minecraft.getInstance().player != null)
				Minecraft.getInstance().player.displayClientMessage(msg, false);
		});
	}

	private static Component clr(String text, TextColor color) {
		return Component.literal(text).setStyle(Style.EMPTY.withColor(color));
	}
}
