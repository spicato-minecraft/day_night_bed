package day_night_bed;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DayNightBed implements ModInitializer {
	public static final String MOD_ID = "day_night_bed";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Day Night Bed initialized");
	}
}
