/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package frostwire.search.yify;

import frostwire.search.AbstractSearchResult;
import frostwire.search.CrawlableSearchResult;

/**
 * @author gubatron
 * @author aldenml
 */
public class YifyTempSearchResult extends AbstractSearchResult implements CrawlableSearchResult {

    private final String itemId;
    private final String detailsUrl;
    private final String displayName;

    public YifyTempSearchResult(String domainName, String itemId, String htmlFilename, String displayName) {
        this.itemId = itemId;
        this.detailsUrl = "https://" + domainName + "/movie/" + itemId + "/" + htmlFilename;
        this.displayName = displayName;
    }

    public String getItemId() {
        return itemId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDetailsUrl() {
        return detailsUrl;
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public boolean isComplete() {
        return false;
    }
}
