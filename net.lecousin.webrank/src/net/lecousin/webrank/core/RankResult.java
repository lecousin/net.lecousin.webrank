package net.lecousin.webrank.core;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.xml.XmlUtil;
import net.lecousin.framework.xml.XmlWriter;

import org.w3c.dom.Element;

public class RankResult {

	RankResult(long time) {
		this.time = time;
	}
	
	private long time;
	private Map<String,Integer> ranks = new HashMap<String,Integer>();
	
	public long getTime() { return time; }
	public Integer getRank(String id) { return ranks.get(id); }
	
	public void setRank(String id, Integer rank) {
		ranks.put(id, rank);
	}
	
	RankResult(Element node) {
		this.time = Long.parseLong(node.getAttribute("time"));
		for (Element e : XmlUtil.get_childs_element(node, "engine")) {
			Integer i = null;
			if (e.hasAttribute("rank"))
				i = Integer.parseInt(e.getAttribute("rank"));
			ranks.put(e.getAttribute("id"), i);
		}
	}
	void save(XmlWriter xml) {
		xml.addAttribute("time", time);
		for (Map.Entry<String, Integer> e : ranks.entrySet()) {
			xml.openTag("engine").addAttribute("id", e.getKey());
			if (e.getValue() != null)
				xml.addAttribute("rank", e.getValue());
			xml.closeTag();
		}
	}
}
