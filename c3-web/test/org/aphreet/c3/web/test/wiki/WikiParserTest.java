package org.aphreet.c3.web.test.wiki;

import java.io.StringWriter;

import junit.framework.TestCase;

import org.aphreet.c3.web.service.impl.wiki.C3HtmlVisitor;

import be.devijver.wikipedia.Parser;
import be.devijver.wikipedia.SmartLink;
import be.devijver.wikipedia.SmartLinkResolver;
import be.devijver.wikipedia.SmartLink.SmartLinkType;

public class WikiParserTest extends TestCase{

	public void testPlainInternalLink(){
		assertTrue(parseWiki("[[SomePage]]").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">SomePage</a>"));
		assertTrue(parseWiki("[[SomePage]] \n").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">SomePage</a>"));
		assertTrue(parseWiki("[[SomePage]]\r\n").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">SomePage</a>"));
		assertTrue(parseWiki("[[SomePage]]\n").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">SomePage</a>"));
		assertTrue(parseWiki("sdfsd [[SomePage]]\n").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">SomePage</a>"));
		assertTrue(parseWiki("[[SomePage]] sfdsf").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">SomePage</a>"));
		assertTrue(parseWiki("[[SomePage]]sfdfsf\n").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">SomePage</a>"));
		assertTrue(parseWiki("sfdfsd[[SomePage]]sfdf\n").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">SomePage</a>"));
	}
	
	public void testAdwancedInternalLink(){
		assertTrue(parseWiki("[[SomePage|Some Another Page]]").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">Some Another Page</a>"));
		assertTrue(parseWiki("[[SomePage|Some Another Page]]\n").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">Some Another Page</a>"));
		assertTrue(parseWiki("[[SomePage|Some Another Page]]\r\n").contains("<a href=\"group/wiki.xhtml?id=1&amp;name=SomePage\">Some Another Page</a>"));
	}
	
	public void testList(){
		String input = "#elem1\n" +
				"#elem2\n" +
				"##elem2.1 \n" +
				"##elem2.2\n" +
				"#elem3\r\n" +
				"##elem3.1\n" +
				"##elem3.2\n\n" +
				"#elem4";
		
		assertTrue(parseOk(input));
	}
	
	public void testTable(){
		String input = "{| border=\"1\" \n" + 
		"|--------------------\r\n" +
		"! header\r\n" + 
        "|-\n" +
        "| row 1, [[SomeLink]]   \n" +
        "| row 1, cell 2\n" +
        "|}\n";
		
		assertTrue(parseOk(input));
	}
	
	public void testItalic(){
		assertTrue(parseOk("''text''\n"));
	}
	
	public void testHeader(){
		assertTrue(parseOk("===header===\ntext here\n"));
	}
	
	private String prepareWikiInput(String input){
		String newInput = input.replaceAll("([^\r])\n", "$1\r\n");
		return newInput;
	}
	
	private boolean parseOk(String input){
		try{
			parseWiki(input);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	private String parseWiki(String input){
		
		StringWriter writer = new StringWriter();
		
		
		new Parser().withVisitor(prepareWikiInput(input), new C3HtmlVisitor(writer, new SmartLinkResolver(){

			@Override
			public SmartLink resolve(String smartLink) {
				return getResourceLink(smartLink);
			}
		}));
		
		String result = writer.toString();
		
		result = result.replaceAll("\r", "").replaceAll("<p></p>", "");
		
		return result;
	}
	
	private SmartLink getResourceLink(String key){
		
			if(key.startsWith("Resource:")){
				String url = "resource.xhtml?id=" + key;
				return new SmartLink(url, key + "_title", SmartLinkType.A_LINK);
			}else{
				if(key.startsWith("Image:")){
					return new SmartLink("download?id=" + "12341234-1234-1234-1234", 
							"123123-name", 
							"resource.xhtml?id=" + "1",
							SmartLinkType.IMG_LINK);
				}
			}
		
		
		return new SmartLink("group/wiki.xhtml?id=" + 1 + "&name=" + key, key, SmartLinkType.A_LINK);
	}
}
