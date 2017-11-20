package tangram.logistic;

import java.util.*;

import tangram.data.Referent;
import weka.core.Attribute;
import weka.core.DenseInstance;

public class ColorModel {

	private int steps;
	private List<Attribute> atts;

	public ColorModel(int steps) {
		this.steps = steps;
		atts = new ArrayList<>();
		for (int i = 0; i < steps; i++) {
			atts.add(new Attribute("color" + i));
		}
	}

	public List<Attribute> getAttributes() {
		return atts;
	}

	public void setValues(Referent ref, DenseInstance instance) {
		for (int i = 0; i < steps; i++) {
			float h = (float)i / 6;
			float dist = Math.min(Math.min(Math.abs(ref.hue - h), Math.abs(ref.hue - (1f+h))), Math.abs((1f+ref.hue) - h));
			float val = (float) Math.pow((1-dist), 3);
			instance.setValue(atts.get(i), val);
		}
	}

}
