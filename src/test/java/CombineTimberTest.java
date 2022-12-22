import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CombineTimberTest {

  @org.junit.jupiter.api.Test
  void optimizeLengths() throws ParseException {

    List<Map<String, String>> testTimber = new ArrayList<Map<String, String>>();
    Map<String, String> val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "1,5" },
        { "no_pieces", "1" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "1" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "1,5" },
        { "no_pieces", "1" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "1" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "3" },
        { "no_pieces", "1" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "1" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    int max_log_length = 4;
    CombineTimber ct = new CombineTimber(max_log_length,0.1);

    Map<String, List<TimberLog>> results = ct.optimizeLengths(testTimber, max_log_length, true);


    assertEquals(1, results.size());
    assertEquals(2, results.get("KR1").size());
    assertEquals(3, results.get("KR1").stream().mapToInt(o -> o.timberItems.stream().mapToInt(ti->ti.getNo_pieces()).sum()).sum());
  }


  @org.junit.jupiter.api.Test
  void optimizeLengthsComplex() throws ParseException {

    List<Map<String, String>> testTimber = new ArrayList<Map<String, String>>();
    Map<String, String> val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "1,5" },
        { "no_pieces", "6" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "1" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "1,2" },
        { "no_pieces", "3" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "3" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "0,75" },
        { "no_pieces", "4" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "4" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "0,60" },
        { "no_pieces", "4" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "3" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "2,7" },
        { "no_pieces", "3" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "1" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "1,1" },
        { "no_pieces", "1" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "5" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "1,9" },
        { "no_pieces", "1" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "3" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);
    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "1,7" },
        { "no_pieces", "1" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "1" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);

    int max_log_length = 3;

    CombineTimber ct = new CombineTimber(max_log_length, 0.1);
    Map<String, List<TimberLog>> results = ct.optimizeLengths(testTimber, max_log_length, true);


    assertEquals(1, results.size());
    assertEquals(12, results.get("KR1").size());
    assertEquals(23, results.get("KR1").stream().mapToInt(o -> o.timberItems.stream().mapToInt(ti->ti.getNo_pieces()).sum()).sum());
  }


  @org.junit.jupiter.api.Test
  void testOversizeItems() throws ParseException {

    List<Map<String, String>> testTimber = new ArrayList<Map<String, String>>();
    Map<String, String> val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "10" },
        { "no_pieces", "3" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "1" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);

    val1 = Stream.of(new String[][] {
        { "ID", "KR1" },
        { "length", "3" },
        { "no_pieces", "3" },
        { "width", "0,16" },
        { "heigth", "0,12" },
        { "note", "..." },
        { "volume", "1" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    testTimber.add(val1);

    int max_log_length = 9;

    CombineTimber ct = new CombineTimber(max_log_length,0.1);

    Map<String, List<TimberLog>> results = ct.optimizeLengths(testTimber, max_log_length, true);

    assertEquals(1, results.size());
    assertEquals(2, results.get("KR1").size());
    assertEquals(3, results.get("KR1").stream().mapToInt(o -> o.timberItems.stream().mapToInt(ti->ti.getNo_pieces()).sum()).sum());
  }
}