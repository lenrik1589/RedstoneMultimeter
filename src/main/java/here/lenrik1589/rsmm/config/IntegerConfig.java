package here.lenrik1589.rsmm.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigBase;
import here.lenrik1589.rsmm.Names;
import net.minecraft.util.math.MathHelper;

public class IntegerConfig extends ConfigBase<IntegerConfig> implements IConfigInteger {
	protected int minValue;
	protected int maxValue;
	protected int defaultValue;
	protected int value;
	private boolean useSlider;

	public IntegerConfig (String name, int defaultValue, String comment) {
		this(name, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE, comment);
	}

	public IntegerConfig (String name, int defaultValue, int minValue, int maxValue, String comment) {
		this(name, defaultValue, minValue, maxValue, false, comment);
	}

	public IntegerConfig (String name, int defaultValue, int minValue, int maxValue, boolean useSlider, String comment) {
		super(ConfigType.INTEGER, name, comment);

		this.minValue = minValue;
		this.maxValue = maxValue;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
		this.useSlider = useSlider;
	}

	@Override
	public boolean shouldUseSlider () {
		return this.useSlider;
	}

	@Override
	public void toggleUseSlider () {
		this.useSlider = !this.useSlider;
	}

	@Override
	public int getIntegerValue () {
		return this.value;
	}

	@Override
	public int getDefaultIntegerValue () {
		return this.defaultValue;
	}

	@Override
	public void setIntegerValue (int value) {
		int oldValue = this.value;
		this.value = this.getClampedValue(value);

		if (oldValue != this.value) {
			this.onValueChanged();
		}
	}

	@Override
	public int getMinIntegerValue () {
		return this.minValue;
	}

	@Override
	public int getMaxIntegerValue () {
		return this.maxValue;
	}

	protected int getClampedValue (int value) {
		return MathHelper.clamp(value, this.minValue, this.maxValue);
	}

	@Override
	public boolean isModified () {
		return this.value != this.defaultValue;
	}

	public void setRange (int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("Minimum value must be less than maximum value.");
		}
		boolean valueChanged = false;
		if (maxValue != max || minValue != min)
			valueChanged = true;
		minValue = min;
		maxValue = max;
		if (valueChanged) {
			value = Math.max(Math.min(value, maxValue), minValue);
			onValueChanged();
		}
	}

	public void setMinValue (int min) {
		if (min >= this.maxValue) {
			throw new IllegalArgumentException("Minimum value must be less than maximum value.");
		}
		boolean valueChanged = false;
		if (minValue != min)
			valueChanged = true;
		minValue = min;
		if (valueChanged) {
			value = Math.max(value, minValue);
			onValueChanged();
		}
	}

	public void setMaxValue (int max) {
		if (this.minValue >= max) {
			throw new IllegalArgumentException("Maximum value must be more than minimum value.");
		}
		boolean valueChanged = false;
		if (maxValue != max)
			valueChanged = true;
		maxValue = max;
		if (valueChanged) {
			value = Math.min(value, maxValue);
			onValueChanged();
		}
	}

	@Override
	public boolean isModified (String newValue) {
		try {
			return Integer.parseInt(newValue) != this.defaultValue;
		} catch (Exception ignored) {
		}

		return true;
	}

	@Override
	public void resetToDefault () {
		this.setIntegerValue(this.defaultValue);
	}

	@Override
	public String getStringValue () {
		return String.valueOf(this.value);
	}

	@Override
	public String getDefaultStringValue () {
		return String.valueOf(this.defaultValue);
	}

	@Override
	public void setValueFromString (String value) {
		try {
			this.setIntegerValue(Integer.parseInt(value));
		} catch (Exception e) {
			Names.LOGGER.warn("Failed to set config value for {} from the string '{}'", this.getName(), value, e);
		}
	}

	@Override
	public void setValueFromJsonElement (JsonElement element) {
		try {
			if (element.isJsonPrimitive()) {
				this.value = this.getClampedValue(element.getAsInt());
			} else {
				Names.LOGGER.warn("Failed to set config value for '{}' from the JSON element '{}'", this.getName(), element);
			}
		} catch (Exception e) {
			Names.LOGGER.warn("Failed to set config value for '{}' from the JSON element '{}'", this.getName(), element, e);
		}
	}

	@Override
	public JsonElement getAsJsonElement () {
		return new JsonPrimitive(this.value);
	}

}
