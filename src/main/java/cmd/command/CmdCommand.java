package cmd.command;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdCommand implements ClientModInitializer {
	public static final String MOD_ID = "cmd-command";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("CmdMod initialized! Use /cmd \"command\" to execute Windows commands.");
	}
}
