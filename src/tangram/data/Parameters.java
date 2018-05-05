package tangram.data;

public class Parameters {
	
	public static boolean USE_RIDGE = true;
	
	public static double RIDGE = 100;

	// Only build model for words with more or equal number of instances than this
	public static int DISCOUNT = 3;
	
	// Only use referring language
	public static boolean ONLY_REFLANG = false;
	
	// Only use language from the giver 
	public static boolean ONLY_GIVER = false;
	
	// Weight score by word frequency 
	public static boolean WEIGHT_BY_FREQ = false;
		
	// Update the model incrementally during testing	
	public static boolean UPDATE_MODEL = false;
	
	// Weight for incremental updates (relative to 1.0 for background model)
	public static double UPDATE_WEIGHT = 1.0;

	public static boolean WEIGHT_BY_POWER = false;

	public static String getSetting() {
		return DISCOUNT + "\t" + ONLY_REFLANG + "\t" + ONLY_GIVER + "\t" + UPDATE_MODEL + "\t" + UPDATE_WEIGHT;
	}
	
}
