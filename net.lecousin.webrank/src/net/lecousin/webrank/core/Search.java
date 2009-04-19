package net.lecousin.webrank.core;

import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedList;

import net.lecousin.framework.collections.SortedList;
import net.lecousin.framework.collections.SortedListTree;
import net.lecousin.framework.time.DateTimeUtil;
import net.lecousin.framework.xml.XmlUtil;
import net.lecousin.framework.xml.XmlWriter;
import net.lecousin.web.search.query.SearchWebQuery;
import net.lecousin.web.search.query.SearchQuery.Language;

import org.w3c.dom.Element;

public class Search {

	Search(long id, SearchWebQuery q) {
		this.id = id;
		this.query = q;
	}
	
	private long id;
	private SearchWebQuery query;
	private SortedList<RankResult> results = new SortedListTree<RankResult>(new Comparator<RankResult>() {
		public int compare(RankResult r1, RankResult r2) {
			return r1.getTime() > r2.getTime() ? 1 : r1.getTime() < r2.getTime() ? -1 : 0;
		}
	});
	
	public long getID() { return id; }
	public SearchWebQuery getQuery() { return query; }
	
	public RankResult getLatestResult() {
		return results.last();
	}
	public void setResult(String id, Integer rank) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		DateTimeUtil.resetHours(c);
		long time = c.getTimeInMillis();
		RankResult r = results.last();
		if (r != null && r.getTime() == time) {
			r.setRank(id, rank);
		} else {
			r = new RankResult(time);
			r.setRank(id, rank);
			results.add(r);
		}
	}
	
	Search(long id, Element root) {
		this.id = id;
		query = new SearchWebQuery();
		Element q = XmlUtil.get_child_element(root, "query");
		if (q != null) {
			query.lang = Language.valueOf(q.getAttribute("lang"));
			LinkedList<String> list = new LinkedList<String>();
			for (Element e : XmlUtil.get_childs_element(q, "all"))
				list.add(XmlUtil.get_inner_text(e));
			query.all = list.toArray(new String[list.size()]);
			list = new LinkedList<String>();
			for (Element e : XmlUtil.get_childs_element(q, "one"))
				list.add(XmlUtil.get_inner_text(e));
			query.one = list.toArray(new String[list.size()]);
			list = new LinkedList<String>();
			for (Element e : XmlUtil.get_childs_element(q, "none"))
				list.add(XmlUtil.get_inner_text(e));
			query.none = list.toArray(new String[list.size()]);
		}
		for (Element e : XmlUtil.get_childs_element(root, "result"))
			results.add(new RankResult(e));
	}
	void save(XmlWriter xml) {
		xml.openTag("query");
		xml.addAttribute("lang", query.lang.toString());
		for (String s : query.all)
			xml.openTag("all").addText(s).closeTag();
		for (String s : query.one)
			xml.openTag("one").addText(s).closeTag();
		for (String s : query.none)
			xml.openTag("none").addText(s).closeTag();
		xml.closeTag();
		for (RankResult r : results) {
			xml.openTag("result");
			r.save(xml);
			xml.closeTag();
		}
	}
}
