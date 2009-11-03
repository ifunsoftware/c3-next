package org.aphreet.c3.web.webbeans.search;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.aphreet.c3.web.entity.Content;
import org.aphreet.c3.web.service.ISearchService;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class SearchBean {

	@Autowired
	private ISearchService searchService;
	
	@HttpParam("q")
	private String query;
	
	private List<SearchResultDTO> foundResources = new LinkedList<SearchResultDTO>();

	private Boolean renderResult = false;
	
	private int scrollerPage;
	
	@PostConstruct
	public void load(){
		if(query != null && query.length() >0){
			List<Content> resources = searchService.searchResources(query);
			for (Content resource : resources) {
				foundResources.add(new SearchResultDTO(resource));
			}
		}
		
		renderResult = !foundResources.isEmpty();
		scrollerPage = 1;
	}
	
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
	
	public List<SearchResultDTO> getFoundResources() {
		return foundResources;
	}

	public void setFoundResources(List<SearchResultDTO> foundResources) {
		this.foundResources = foundResources;
	}

	public Boolean getRenderResult() {
		return renderResult;
	}

	public void setRenderResult(Boolean renderResult) {
		this.renderResult = renderResult;
	}
	
	public int getScrollerPage() {
		return scrollerPage;
	}

	public void setScrollerPage(int scrollerPage) {
		this.scrollerPage = scrollerPage;
	}

	public class SearchResultDTO{
		
		private int id;
		
		private String title;
		
		private String author;
		
		private String description;
		
		public SearchResultDTO(Content resource){
			this.id = resource.getId();
			this.title = resource.getTitle();
			this.author = "";//resource.getMetadata().getMetadataValue(Metadata.AUTHOR);
			this.description = "";//resource.getMetadata().getMetadataValue(Metadata.COMMENT);
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
