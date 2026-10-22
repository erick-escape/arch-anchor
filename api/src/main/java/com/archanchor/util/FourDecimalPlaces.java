package com.archanchor.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The precision of every number the API writes (#71): at most four decimal places,
 * rounded half up. Similarities and rates are fractions, and the web shows at most four
 * places of one (two of a percentage), so the other twelve digits only made the JSON hard
 * to read. The analyses, the filters and the stored files keep full precision.
 *
 * <pre>
 * FourDecimalPlaces.round(0.4444444444444444); // 0.4444
 * new ObjectMapper().registerModule(FourDecimalPlaces.jacksonModule());
 * </pre>
 */
public final class FourDecimalPlaces {

	private static final int PLACES = 4;

	private FourDecimalPlaces() {
	}

	/**
	 * {@code value} rounded half up to four decimal places; NaN and infinities as they
	 * are.
	 */
	public static double round(double value) {
		return Double.isFinite(value) ? rounded(value).doubleValue() : value;
	}

	/** A Jackson module that writes every {@code double} and {@code Double} rounded. */
	public static Module jacksonModule() {
		RoundedDoubleSerializer serializer = new RoundedDoubleSerializer();
		return new SimpleModule("FourDecimalPlaces").addSerializer(Double.class, serializer)
			.addSerializer(Double.TYPE, serializer);
	}

	// Written as a BigDecimal, which prints plainly at this scale: Double.toString would
	// write 0.0001 as 1.0E-4. One decimal place at least, so that 1.0 does not become 1.
	private static BigDecimal rounded(double value) {
		BigDecimal rounded = BigDecimal.valueOf(value).setScale(PLACES, RoundingMode.HALF_UP).stripTrailingZeros();
		return rounded.scale() < 1 ? rounded.setScale(1) : rounded;
	}

	private static final class RoundedDoubleSerializer extends StdSerializer<Double> {

		private RoundedDoubleSerializer() {
			super(Double.class);
		}

		@Override
		public void serialize(Double value, JsonGenerator generator, SerializerProvider provider) throws IOException {
			if (!Double.isFinite(value)) {
				generator.writeNumber(value);
				return;
			}
			generator.writeNumber(rounded(value));
		}

	}

}
