package day_night_bed.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import day_night_bed.DayNightBed;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side config at {@code config/day_night_bed.json}.
 */
public final class DayNightBedConfig {
	private static final Logger LOGGER = LogManager.getLogger(DayNightBed.MOD_ID + ".config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = DayNightBed.MOD_ID + ".json";
	private static volatile DayNightBedConfig instance;

	/** Master toggle — when false, mod behavior matches vanilla. */
	public boolean enabled = true;
	/** Target world time after night→day or storm skip (default matches {@code /time set day}). */
	public long dayTargetTick = 1_000L;
	/** Target world time after day→night (default matches {@code /time set night}). */
	public long nightTargetTick = 13_000L;
	/** When true, day→night sleep resets {@code TimeSinceRest} like vanilla night sleep. */
	public boolean resetRestStatOnDayRest = false;

	public static DayNightBedConfig get() {
		DayNightBedConfig current = instance;
		if (current == null) {
			current = load();
			instance = current;
		}
		return current;
	}

	public static void set(DayNightBedConfig config) {
		instance = config;
	}

	/** Test-only override; does not write to disk. */
	public static void setForTest(DayNightBedConfig config) {
		instance = config;
	}

	public static DayNightBedConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		DayNightBedConfig config = new DayNightBedConfig();
		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				DayNightBedConfig fromFile = GSON.fromJson(reader, DayNightBedConfig.class);
				if (fromFile != null) {
					config = fromFile;
				}
			} catch (IOException e) {
				LOGGER.error("Failed to read config {}, using defaults", path, e);
			}
		} else {
			try {
				Files.createDirectories(path.getParent());
				config.save();
			} catch (IOException e) {
				LOGGER.warn("Could not write default config to {}", path, e);
			}
		}
		return config;
	}

	public void save() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(this, writer);
		} catch (IOException e) {
			LOGGER.error("Failed to save config {}", path, e);
		}
	}

	public static DayNightBedConfig defaults() {
		return new DayNightBedConfig();
	}
}
