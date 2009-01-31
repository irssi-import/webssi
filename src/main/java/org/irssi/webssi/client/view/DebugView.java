package org.irssi.webssi.client.view;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;

public class DebugView extends SimplePanel {
	private final Element tbody;	
	
	public DebugView() {
		super(DOM.createTable());

		Element table = getElement();
		table.setAttribute("cellPadding", "0");
		table.setAttribute("cellSpacing", "0");

		tbody = DOM.createTBody();
		DOM.appendChild(getElement(), tbody);
	}
	
	private Element createRow(String left, String right) {
		Element tr = DOM.createTR();
		Element ltd = DOM.createTD();
		Element rtd = DOM.createTD();

		DOM.appendChild(tr, ltd);
		DOM.appendChild(tr, rtd);

		DOM.setInnerText(ltd, left);
		DOM.setInnerText(rtd, right);

		return tr;
	}
	
	void debug(String type, String message) {
		Element row = createRow("[" + type + "] ", message);
		DOM.appendChild(tbody, row);
	}
}
