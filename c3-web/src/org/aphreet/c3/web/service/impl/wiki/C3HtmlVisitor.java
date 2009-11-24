package org.aphreet.c3.web.service.impl.wiki;

import java.io.Writer;

import org.apache.commons.lang.StringEscapeUtils;

import be.devijver.wikipedia.SmartLink;
import be.devijver.wikipedia.SmartLinkResolver;
import be.devijver.wikipedia.html.HtmlEncoder;
import be.devijver.wikipedia.html.HtmlVisitor;

public class C3HtmlVisitor extends HtmlVisitor{

	public C3HtmlVisitor(Writer writer, SmartLinkResolver smartLinkResolver) {
		
		super(writer, smartLinkResolver, new C3HtmlEncoder(), true);
	}
	
	public static class C3HtmlEncoder implements HtmlEncoder{

		@Override
		public String encode(String s) {
			String result = StringEscapeUtils.unescapeHtml(s);
			// return StringEscapeUtils.escapeHtml(result);
			return escape(result);
		}

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
	}
	
	private boolean inImage = false;
	private boolean inHeader = false;
	
	@Override
	public void handleString(String s) {
		if (inImage) {
			String[] params = s.split("\\|");
			if (params[0].matches("^[\\d]+px$")) {
				output.append(" width=\"" + params[0] + "\"");
			}
			if (params.length > 1) {
				output.append(" alt=\"" + characterEncoder.encode(params[1])
						+ "\"");
			}
			output.append("/>");
			inImage = false;
		} else {
			
			if(inHeader){
				output.append(characterEncoder.encode(s));
				output.append("\">");
				inHeader = false;
			}
			
			output.append(characterEncoder.encode(s));
		}
	}
	
	@Override
	public void startSmartLinkWithCaption(String s) {
		SmartLink resolvedLink = resolveSmartLink(s);
		if (resolvedLink.isImage()) {
			inImage = true;
			output.append("<a href=\""
					+ characterEncoder.encode(resolvedLink.getResourceLink())
					+ "\"><img src=\""
					+ characterEncoder.encode(resolvedLink.getUrl()) + "\" class=\"wiki_image\"");
		} else {
			output.append("<a href=\""
					+ characterEncoder.encode(resolvedLink.getUrl()) + "\">");
		}

	}
	
	@Override
	public void handleSmartLinkWithoutCaption(String string) {
		SmartLink resolvedLink = resolveSmartLink(string);
		if (resolvedLink.isImage()) {
			output.append("<a href=\""
					+ characterEncoder.encode(resolvedLink.getResourceLink())
					+ "\"><img src=\""
					+ characterEncoder.encode(resolvedLink.getUrl())
					+ "\" class=\"wiki_image\"/></a>");
		} else {
			output.append("<a href=\""
					+ characterEncoder.encode(resolvedLink.getUrl()) + "\">"
					+ resolvedLink.getName() + "</a>");
		}

	}
	
	private SmartLink resolveSmartLink(String s) {
		SmartLink resolvedLink = null;
		if (smartLinkResolver != null) {
			resolvedLink = smartLinkResolver.resolve(s);
		} else {
			throw new RuntimeException("Could not resolve smart link [" + s
					+ "] because SmartLinkResolver is null!");
		}
		if (resolvedLink == null) {
			throw new RuntimeException("SmartLinkResolver ["
					+ smartLinkResolver + "] could not resolved smart link ["
					+ s + "]!");
		}
		return resolvedLink;
	}
	
	@Override
	public void startDocument() {
	}
	
	@Override
	public void endDocument() {
		output.flush();
		output.finished();
	}

	public void startHeading1() {
		output.append("<h1 id=\"");
		inHeader = true;
	}

	public void startHeading2() {
		output.append("<h2 id=\"");
		inHeader = true;
	}

	public void startHeading3() {
		output.append("<h3 id=\"");
		inHeader = true;
	}

	public void startHeading4() {
		output.append("<h4 id=\"");
		inHeader = true;
	}

	public void startHeading5() {
		output.append("<h5 id=\"");
		inHeader = true;
	}

	public void startHeading6() {
		output.append("<h6 id=\"");
		inHeader = true;
	}

}
