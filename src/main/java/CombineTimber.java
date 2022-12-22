import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;


/**
 * This Application can combine small Timber Items export from CAD sofware to a bigger Timber Logs, that can be cut in the saw mill.
 */
public class CombineTimber {

  public static final String COLUMN_LENGTH = "length";
  public static final String COLUMN_WIDTH = "width";
  public static final String COLUMN_HEIGTH = "heigth";
  public static final String COLUMN_NO_PIECES = "no_pieces";
  static String COLUMN_ID = "ID";
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CombineTimber.class);
  private final Integer max_Log_length;
  public static Double added_percentage = 0.0;
  private List<TimberItem> missingTimberItems = new ArrayList<>();


  CombineTimber(Integer max_Log_length, Double added_percentage) {
    this.max_Log_length = max_Log_length;
    CombineTimber.added_percentage = added_percentage;

  }

  /**
   * Method to parse CSV with all needed Timber Items. The structure supported now is fixed as follows: ID, Length, No Pieces, Width, Height, Note,
   * Volume
   *
   * @param fileAsInputStream InputStream of the CSV.
   * @return List of Maps, where the entryset of the map are the parsed columns of the csv. Each item in List is the row of the CSV.
   * @throws JsonProcessingException
   * @throws IOException
   */
  public List<Map<String, String>> readCsv(InputStream fileAsInputStream) throws JsonProcessingException, IOException {
    List<Map<String, String>> response = new LinkedList<Map<String, String>>();
    CsvMapper mapper = new CsvMapper();
    CsvSchema schema = CsvSchema.builder()
        .addColumn(COLUMN_ID)
        .addColumn(COLUMN_LENGTH)
        .addColumn(COLUMN_NO_PIECES)
        .addColumn(COLUMN_WIDTH)
        .addColumn(COLUMN_HEIGTH)
        .addColumn("note")
        .addColumn("volume")
        .setColumnSeparator(';')
        .build().withHeader();
    MappingIterator<Map<String, String>> iterator = mapper.reader(Map.class)
        .with(schema)
        .readValues(fileAsInputStream);
    while (iterator.hasNext()) {
      response.add(iterator.next());
    }
    return response;
  }


  /**
   * Method to optimize the <code>neededTimber</code> items.
   *
   * @param neededTimber         list of maps, where the map represents the parsed columns from the CSV. So each row is 1 entryset in the map.
   * @param maximumTimberLogSize Maximum Length of Timber Log that Mill Saw can create (e.g. 13 for 13 meters)
   * @param useIdToFindSameSize  If true, method will not use only ID to check the same Timber Items, but will look across different IDs and find the
   *                             Timber Items with same width and height.
   * @return Map of ID (e.g. KVH120x60) and list of Timber Logs that should be used for this ID to satisfy the needed Timber Items.
   * @throws java.text.ParseException
   */
  public Map<String, List<TimberLog>> optimizeLengths(List<Map<String, String>> neededTimber, int maximumTimberLogSize,
      Boolean useIdToFindSameSize) throws java.text.ParseException {
    // TODO use the useIdToFindSameSize parameter to decide if to use ID or to use also widthxheight to check different ID but with same size.
    //Get All with same width/height
    // or use the ID to decide
    Map<String, List<TimberLog>> finalOptimizedLengths = new HashMap<>();

    //Create the list with  Objects instead of Map
    List<TimberItem> needTimberItems = neededTimber.stream().map(t -> {
      try {
        return new TimberItem(t);
      } catch (java.text.ParseException e) {
        e.printStackTrace();
        return null;
      }
    }).collect(Collectors.toList());

    printOrigStats(needTimberItems);

    //Sort by the length.
    //Take first the longest pieces and try to fit them into TimberLogs.

    Comparator<TimberItem> byLength = Comparator.comparing(TimberItem::getLength);
    Collections.sort(needTimberItems, Collections.reverseOrder(byLength));

    // Iterate through all timber items froms CSV
    for (TimberItem timberItem : needTimberItems) {
      if (!finalOptimizedLengths.containsKey(timberItem.getID())) {
        //if this ID does not exist, create new Timber Log from this Timber Item (if more pieces are required, multiple Logs can be created)
        finalOptimizedLengths.put(timberItem.getID(), combineItemsToNewLog(timberItem, maximumTimberLogSize));

      } else {
        combineItemsWithExistingLogs(finalOptimizedLengths, timberItem, maximumTimberLogSize);
      }

    }

    return finalOptimizedLengths;
  }


  /**
   * Combines <code>timberItem</code> into the existing TimberLogs given in <code>finalOptimizedLengths</code> or creates new Timber Logs that are
   * added to the <code>finalOptimizedLengths</code>.
   *
   * @param finalOptimizedLengths Map where key is TImber Item ID,
   * @param timberItem the Timber Item to be combined into the Timber Logs
   * @param maximumTimberLogSize Maximum Length of Timber Log that Mill Saw can create (e.g. 13 for 13 meters)
   * @return
   */
  private List<TimberLog> combineItemsWithExistingLogs(Map<String, List<TimberLog>> finalOptimizedLengths, TimberItem timberItem,
      int maximumTimberLogSize) {
    //Get the existing Timber Logs for this ID.
    List<TimberLog> existingTimberLogs = finalOptimizedLengths.get(timberItem.getID());
    List<TimberLog> newTimberLogs = new ArrayList<>();
    // Check if it fits the first if not, create new
    boolean addedLastTimerItem = false;
    for (int i = 0; i < timberItem.getNo_pieces(); i++) {
      addedLastTimerItem = checkExistingLog(existingTimberLogs, timberItem);
      if (!addedLastTimerItem) {
        //The timberItem does not fit anything in the existing timberLogs, creating a new one.
        TimberItem tiTemp = new TimberItem(timberItem.getID(), timberItem.getLength(), timberItem.getWidth(), timberItem.getHeigth(), 1);
        boolean addedToNewLogs = checkExistingLog(newTimberLogs, timberItem);
        if (!addedToNewLogs) {
          newTimberLogs.addAll(combineItemsToNewLog(tiTemp, maximumTimberLogSize));
        }
      }
    }

    existingTimberLogs.addAll(newTimberLogs);

    return existingTimberLogs;
  }

  /**
   * Method to check <code>existingTimberLogs</code> if the <code>timberItem</code> fits to any of these existing ones. <br/> This method adds just 1
   * Item. so it is expected the <code>timberItem</code> contains no_pieces == 1. Or if it contains different number, the number of pieces is not
   * being checked.
   *
   * @param existingTimberLogs TimberLogs to be checked if <code>timberItem</code> fits into any of these.
   * @param timberItem         The item to be checked if it fits. no_pieces attribute is ignored, it just takes 1 piece of the Item.
   * @return true if the Timber Item fitted and was added. False if it does not fit.
   */
  private boolean checkExistingLog(List<TimberLog> existingTimberLogs, TimberItem timberItem) {
    boolean addedLastTimerItem = false;
    for (TimberLog timberLog : existingTimberLogs) {
      //TODO this function can be put to TimberLog, where it will add the 10%
      if (timberLog.isTimberItemFitting(timberItem)) {
        //It fits the Timber Log
        TimberItem tiTemp = new TimberItem(timberItem.getID(), timberItem.getLength(), timberItem.getWidth(), timberItem.getHeigth(), 1);
        timberLog.addTimberItem(tiTemp);
        addedLastTimerItem = true;
        break;
      } else {
        //It does not fit, go to next one.
        addedLastTimerItem = false;
      }
    }
    return addedLastTimerItem;
  }

  /**
   * Method to combine the given <code>timberItem</code> into a new Logs. <br/> This method is called when the ID of the <code>timberItem</code> has
   * not been found.
   *
   * @param timberItem        Timber Item that will be combined into Timber Logs. The attribute no_pieces is being used to create multiple logs if
   *                          needed.
   * @param maximumTimberSize Maximum size of the Timber Log that Saw Mill can handle.
   * @return List of TimberLogs that have been created for the given Timber Item.
   */
  private List<TimberLog> combineItemsToNewLog(TimberItem timberItem, int maximumTimberSize) {

    List<TimberLog> newTimberLogs = new ArrayList<>();
    TimberLog tl = new TimberLog(maximumTimberSize, 0.0);

    boolean lastTimberLogAdded = false;

    for (int i = 0; i < timberItem.getNo_pieces(); i++) {
      //For each piece, check if it still fits the TimberLog
      try {
        if (tl.isTimberItemFitting(timberItem)) {
          //add the item
          TimberItem tiTemp = new TimberItem(timberItem.getID(), timberItem.getLength(), timberItem.getWidth(), timberItem.getHeigth(), 1);
          tl.addTimberItem(tiTemp);
        } else {
          //put the existing TimerLog to final list
          if (tl.timberItems.size() > 0) {
            newTimberLogs.add(tl);
          }
          //create new TimberLog, because it does not fit.
          tl = new TimberLog(maximumTimberSize, 0.0);
          TimberItem tiTemp = new TimberItem(timberItem.getID(), timberItem.getLength(), timberItem.getWidth(), timberItem.getHeigth(), 1);
          tl.addTimberItem(tiTemp);
        }
      } catch (IndexOutOfBoundsException e) {
        TimberItem tiTemp = new TimberItem(timberItem.getID(), timberItem.getLength(), timberItem.getWidth(), timberItem.getHeigth(), 1);
        missingTimberItems.add(tiTemp);
      }

    }
    if (tl.timberItems.size() > 0) {
      //Only add Timber Log if there are nay pieces, otherwise does not make sense.
      newTimberLogs.add(tl);
    }

    return newTimberLogs;
  }

  /**
   * Prints the Final list of Timber Logs that have been combined from the Timber Items
   *
   * @param optimizedLengths
   */
  public void printFinalList(Map<String, List<TimberLog>> optimizedLengths) {
    LOGGER.info("Combined Timber Items with Maximum Timber Log Length {} and Cros Cut Percentage of {} %.", max_Log_length,
        CombineTimber.added_percentage * 100);

    for (String ID : optimizedLengths.keySet()) {

      LOGGER.info("\tID: {}", ID);
      LOGGER.info("\t\tTotal Logs: {}", optimizedLengths.get(ID).size());
      Double total_length = optimizedLengths.get(ID).stream().mapToDouble(tl -> tl.maximumLength).sum();
      Double total_occupied_length = optimizedLengths.get(ID).stream().mapToDouble(tl -> tl.occupiedSize).sum();
      LOGGER.info("\t\tTotal Length {}, total Occupied Length: {}", total_length, total_occupied_length);
      Integer total_no_pieces = optimizedLengths.get(ID).stream()
          .mapToInt(tl -> tl.timberItems.stream().mapToInt(ti -> ti.getNo_pieces()).sum()).sum();
      Integer total_no_pieces2 = 0;
      for (TimberLog tl : optimizedLengths.get(ID)) {
        for (TimberItem ti : tl.timberItems) {
          total_no_pieces2 += ti.getNo_pieces();
        }
      }
      LOGGER.info("\t\tTotal No_Pieces: {}, another no pieces: {}", total_no_pieces, total_no_pieces2);
    }

    if (missingTimberItems.size() > 0) {
      LOGGER.warn("Total of {} Missing Timber Items!", missingTimberItems.size());
      for (TimberItem timberItem : missingTimberItems) {
        LOGGER.info("\tID: {}", timberItem.getID());
        LOGGER.info("\t\tTimber Item Length {}, Timer number of pieces: {}", timberItem.getLength(), timberItem.getNo_pieces());
      }
    }

  }

  /**
   * Prints the Statistics of the needed Timber Items from the CSV.
   *
   * @param needTimberItems
   */
  private void printOrigStats(List<TimberItem> needTimberItems) {
    Integer totalPieces = 0;
    for (TimberItem ti : needTimberItems) {
      totalPieces += ti.getNo_pieces();
    }

    LOGGER.info("Total of Original Pieces {}", totalPieces);
  }

  public static void main(String[] args) throws IOException, java.text.ParseException, ParseException {

    Options options = new Options();

    Option input = new Option("i", "input", true, "input file path with list of Timber");
    input.setRequired(true);
    options.addOption(input);

    Option maxLogLengthOption = new Option("m", "max_log_length", true, "Maximum size of the Timber Log (e.g. 9 for 9 meters)");
    maxLogLengthOption.setRequired(false);
    maxLogLengthOption.setType(Number.class);
    options.addOption(maxLogLengthOption);
    Option addedPercentageOption = new Option("p", "add_percentage", true,
        "Amount of Cross cut (estiamted waste) in percentages (e.g. 0.1 for 10% of Cross Cut).");
    addedPercentageOption.setRequired(false);
    addedPercentageOption.setType(Number.class);
    options.addOption(addedPercentageOption);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null;//not a good practice, it serves it purpose

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }

    String inputFilePath = cmd.getOptionValue("input");
    int max_Log_length = 9;
    Object tempMax = cmd.getParsedOptionValue("max_log_length");
    if (tempMax != null) {
      max_Log_length = ((Long) tempMax).intValue();
    }
    Object tempPerc = cmd.getParsedOptionValue("add_percentage");
    Double added_percentage = 0.0;
    if (tempPerc != null) {
      added_percentage = (Double) tempPerc;
    }

    InputStream inputFileStream = new FileInputStream(inputFilePath);

    CombineTimber ct = new CombineTimber(max_Log_length, added_percentage);

    List<Map<String, String>> values = ct.readCsv(inputFileStream);

    Map<String, List<TimberLog>> optimizedLengths = ct.optimizeLengths(values, max_Log_length, false);

    ct.printFinalList(optimizedLengths);

  }

}


