package org.irssi.webssi.client.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.SimplePanel;

class BufferView extends SimplePanel {
	private final TableSectionElement tbody;

	BufferView() {
		super(DOM.createTable());

		TableElement table = getElement().cast();
		table.setAttribute("cellPadding", "0");
		table.setAttribute("cellSpacing", "0");
		
		tbody = Document.get().createTBodyElement();
		getElement().appendChild(tbody);
		this.setStylePrimaryName("WinView");
	}

	void textPrinted(String text) {
		TableRowElement tr = Document.get().createTRElement();
		TableCellElement td = Document.get().createTDElement();
		tr.appendChild(td);
		td.setInnerHTML(text);
		tbody.appendChild(tr);
	}
}
