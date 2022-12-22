import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

public class TimberLog {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TimberLog.class);
  int maximumLength;
  List<TimberItem> timberItems;
  Double occupiedSize;

  public TimberLog(Double heigth, Double width, Double maximumLength, int i) {
  }

  public TimberLog(int maximumTimberSize, Double occupiedSize) {
    this.maximumLength = maximumTimberSize;
    this.occupiedSize = occupiedSize;
    this.timberItems = new ArrayList<>();
  }

  public void addTimberItem(TimberItem timberItem) throws IndexOutOfBoundsException {
    if (isTimberItemFitting(timberItem)) {
      this.timberItems.add(timberItem);
      this.occupiedSize = this.occupiedSize + (timberItem.getLength() * (1 + CombineTimber.added_percentage));
    } else {
      String message = MessageFormat.format("Timer Item {0} with Length {1} does not fit Timber Log with Max Size {2} (occupied Length {3}).",
          timberItem.getID(), timberItem.getLength(), maximumLength, occupiedSize);
      LOGGER.warn(message);
      throw new IndexOutOfBoundsException(message);
    }
  }

  public Boolean isTimberItemFitting(TimberItem timberItem) {
    return (this.occupiedSize + (timberItem.getLength() * (1 + CombineTimber.added_percentage)) <= this.maximumLength);
  }
}
