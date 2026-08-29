package dairy.erp.ui;

import dairy.erp.ui.dialogs.CalendarDialog;
import dairy.erp.util.DateUtil;

import javax.swing.JButton;
import java.awt.Frame;

/**
 * A date picker that allows selection of future dates.
 * Use this for fields like "Effective From" / "Effective To" in Rate Chart
 * where future dates need to be selected.
 */
public class DatePickerFuture extends DatePicker {

    @Override
    protected void openDialog() {
        Frame owner = findOwner();
        // Open calendar with NO date restrictions (min=null, max=null)
        CalendarDialog dlg = new CalendarDialog(owner, getDate(), null, null);
        dlg.setVisible(true);
        java.time.LocalDate picked = dlg.getSelectedDate();
        if (picked != null) {
            setDate(picked);
        }
    }
}
