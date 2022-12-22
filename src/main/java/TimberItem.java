import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class TimberItem {

  private String ID;
  private Double length;
  private Double width;
  private Double heigth;
  private int no_pieces;

  public String getID() {
    return ID;
  }

  public Double getLength() {
    return length;
  }

  public Double getWidth() {
    return width;
  }

  public Double getHeigth() {
    return heigth;
  }

  public int getNo_pieces() {
    return no_pieces;
  }

  public TimberItem(String ID, Double length, Double width, Double height, int no_pieces) {
    this.ID = ID;
    this.length = length;
    this.width = width;
    this.heigth = height;
    this.no_pieces = no_pieces;

  }

  public TimberItem(Map<String, String> timberItemMap) throws ParseException {

    ID = timberItemMap.get(CombineTimber.COLUMN_ID);
    NumberFormat nf = NumberFormat.getInstance(Locale.FRANCE);
    length = nf.parse(timberItemMap.get(CombineTimber.COLUMN_LENGTH)).doubleValue();
    width = nf.parse(timberItemMap.get(CombineTimber.COLUMN_WIDTH)).doubleValue();
    heigth = nf.parse(timberItemMap.get(CombineTimber.COLUMN_HEIGTH)).doubleValue();
    no_pieces = Integer.parseInt(timberItemMap.get(CombineTimber.COLUMN_NO_PIECES));


  }
}
