package ca.bc.gov.fw.wildlifetracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Number picker custom view
 */
public class WTNumberPicker extends LinearLayout {

    /**
     * Minimum value allowed. Defaults to 0.
     */
    public int minValue = 0;

    /**
     * Maximum value allowed. Defaults to 99.
     */
    public int maxValue = 99;

    private Button minusButton_;
    private Button plusButton_;
    private TextView numberTextView_;
    private int value_;

    public WTNumberPicker(Context context) {
        super(context);
        init(context);
    }

    public WTNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        if (isInEditMode())
            return;
        View.inflate(context, R.layout.number_picker, this);
        minusButton_ = (Button) findViewById(R.id.minusButton);
        plusButton_ = (Button) findViewById(R.id.plusButton);
        numberTextView_ = (TextView) findViewById(R.id.tvNumber);
        setValue(0);
        minusButton_.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (value_ > minValue) {
                    setValue(value_ - 1);
                }
            }
        });
        plusButton_.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (value_ < maxValue) {
                    setValue(value_ + 1);
                }
            }
        });
    }

    public int getValue() {
        return value_;
    }

    public void setValue(int value) {
        if (value > maxValue)
            value_ = maxValue;
        else if (value < minValue)
            value_ = minValue;
        else
            value_ = value;

        numberTextView_.setText(String.valueOf(value_));
        minusButton_.setEnabled(value_ > minValue);
        plusButton_.setEnabled(value_ < maxValue);
    }

    public void setMinValue(int minValue) {
        if (minValue > value_) {
            setValue(minValue);
        }
        this.minValue = minValue;
    }

    public void setMaxValue(int maxValue) {
        if (maxValue < value_) {
            setValue(maxValue);
        }
        this.maxValue = maxValue;
    }
}
