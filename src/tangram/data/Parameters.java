package tangram.data;

import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parameters {
	
	// Only build model for words with more or equal number of instances than this
	public static int DISCOUNT = 3;
	
	// Only use referring language
	public static boolean ONLY_REFLANG = false;
	
	// Only use language from the giver 
	public static boolean ONLY_GIVER = false;
	
	// Weight score by word frequency 
	public static boolean WEIGHT_BY_FREQ = true;
		
	// Update the model incrementally during testing	
	public static boolean UPDATE_MODEL = false;
	
	// Weight for incremental updates (relative to 1.0 for background model)
	public static double UPDATE_WEIGHT = 1.0;

	public static String getSetting() {
		return Stream.of(DISCOUNT, ONLY_REFLANG, ONLY_GIVER, UPDATE_MODEL, UPDATE_WEIGHT).map(Object::toString).collect(ROW_CELL_JOINER);
	}
	
	private static final String HEADER;
	
	private static final Collector<CharSequence,?,String> ROW_CELL_JOINER = Collectors.joining("\t");
	
	static {
		HEADER = Stream.of("DISCOUNT", "ONLY_REFLANG", "ONLY_GIVER", "UPDATE_MODEL", "UPDATE_WEIGHT").collect(ROW_CELL_JOINER);
	}

	/**
	 * @return the header
	 */
	public static String getHeader() {
		return HEADER;
	}
	
}
