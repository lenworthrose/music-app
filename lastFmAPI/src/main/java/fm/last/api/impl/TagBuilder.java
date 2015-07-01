/***************************************************************************
 *   Copyright 2005-2009 Last.fm Ltd.                                      *
 *   Portions contributed by Casey Link, Lukasz Wisniewski,                *
 *   Mike Jennings, and Michael Novak Jr.                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.         *
 ***************************************************************************/
package fm.last.api.impl;

import org.w3c.dom.Node;

import fm.last.api.Tag;
import fm.last.xml.XMLBuilder;

/**
 * @author Casey Link
 */
public class TagBuilder extends XMLBuilder<Tag> {

	@Override
	public Tag build(Node tagNode) {
		node = tagNode;
		String name = getText("name");
		String tagcountStr = getText("count");
		int tagcount = -1;
		try {
			tagcount = Integer.parseInt(tagcountStr);
		} catch (NumberFormatException e) {
		} // ignore failed tagcount parsing
		String url = getText("url");
		return new Tag(name, tagcount, url);
	}

}
