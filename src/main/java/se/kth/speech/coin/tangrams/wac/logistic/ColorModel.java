/*******************************************************************************
 * Copyright 2017 Todd Shore
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package se.kth.speech.coin.tangrams.wac.logistic;

import java.util.*;

import se.kth.speech.coin.tangrams.wac.data.Referent;
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
			float dist = Math.min(Math.min(Math.abs(ref.getHue() - h), Math.abs(ref.getHue() - (1f+h))), Math.abs((1f+ref.getHue()) - h));
			float val = (float) Math.pow((1-dist), 3);
			instance.setValue(atts.get(i), val);
		}
	}

}
