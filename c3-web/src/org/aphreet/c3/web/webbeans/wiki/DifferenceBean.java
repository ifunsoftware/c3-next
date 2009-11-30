package org.aphreet.c3.web.webbeans.wiki;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aphreet.c3.web.entity.WikiPage;
import org.aphreet.c3.web.entity.WikiPageVersion;
import org.aphreet.c3.web.service.IWikiService;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.webbeans.group.IdGroupViewBean;
import org.aphreet.springframework.web.HttpParam;
import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;
import org.springframework.beans.factory.annotation.Autowired;

public class DifferenceBean extends IdGroupViewBean{

	@HttpParam("name")
	private String pageName;
	
	@HttpParam("rev")
	private Integer revision;
	
	@HttpParam("rev2")
	private Integer revision2;

	@Autowired
	private IWikiService wikiService;
	
	private WikiPage wikiPage;
	
	private String text;
	
	@Override
	protected void load() {
		wikiPage = wikiService.getPage(group, pageName);
		if(wikiPage != null){
			
			WikiPageVersion version = wikiService.getPageVersion(wikiPage, revision);
			WikiPageVersion version2 = wikiService.getPageVersion(wikiPage, revision2);
			
			if(version != null && version2 != null){
				String text1 = escape(version.getBody());
				String text2 = escape(version2.getBody());
				
				text = getDifference(text1, text2);
				
			}else{
				HttpUtil.sendNotFound();
			}
		}else{
			HttpUtil.sendNotFound();
		}
	}
	
	//TODO replace to StringBuilder
	private String escape(String source) {
		if (source == null) {
			return null;
		}
		String result = new String();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
			case 34: // "
				result = result + "&quot;";
				break;
			case 38:// &
				result = result + "&amp;";
				break;
			case 60:// <
				result = result + "&lt;";
				break;
			case 62:// >
				result = result + "&gt;";
				break;
			default:
				result = result + c;
				break;
			}
		}
		return result;
	}
	
	private String getDifference(String text1, String text2){
		List<String> first = new ArrayList<String>(Arrays.asList(text1.split("\r?\n")));
		List<String> second = Arrays.asList(text2.split("\r?\n"));
		
		
		Diff<String> diff = new Diff<String>(first, second);
		
		int insertedCount = 0;
		
		for(Difference d : diff.diff()){
			
			
			
			for(int i=d.getDeletedStart(); i<= d.getDeletedEnd(); i++){
				
				String replacement = first.get(i + insertedCount);
				
				if(i == d.getDeletedStart()){
					replacement = "<span class=\"diff_removed\">" + replacement;
				}
				
				if(i == d.getDeletedEnd()){
					replacement = replacement + "</span>";
				}
				
				first.set(i + insertedCount, replacement);
			}
			
			
			int insertStart = d.getDeletedEnd()+1;
			
			if(d.getDeletedEnd() == -1){
				insertStart = d.getAddedStart();
			}
			
			int insertedBeforeDiff = insertedCount;
			
			
			
			for(int i=d.getAddedStart(), j=0; i<= d.getAddedEnd(); i++,j++){
				
				String addStr = second.get(i);
				
				if(i == d.getAddedStart()){
					addStr =  "<span class=\"diff_added\">" + addStr;
				}
				
				if(i == d.getAddedEnd()){
					addStr = addStr + "</span>";
				}
				
				
				int insertPlace = insertStart + j + insertedBeforeDiff;
				
				if(insertPlace > first.size()){
					first.add(addStr);
				}else{
					first.add(insertStart + j + insertedBeforeDiff, addStr) ;
				}
				insertedCount++;
			}
		}
		
		StringBuilder builder = new StringBuilder();
		
		for(String line : first){
			builder.append(line);
			if(!line.endsWith("</span>")){
				builder.append("\n");
			}
		}
		
		return builder.toString();
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	public Integer getRevision2() {
		return revision2;
	}

	public void setRevision2(Integer revision2) {
		this.revision2 = revision2;
	}

	public WikiPage getWikiPage() {
		return wikiPage;
	}

	public String getText() {
		return text;
	}
}
