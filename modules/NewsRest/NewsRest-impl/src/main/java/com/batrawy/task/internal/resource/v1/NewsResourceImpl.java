package com.batrawy.task.internal.resource.v1;

import com.batrawy.task.dto.v1.NewsEntry;
import com.batrawy.task.resource.v1.NewsResource;
import com.liferay.journal.service.JournalFolderLocalServiceUtil;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.cache.PortalCacheManager;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.*;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ahmed
 */
@Component(
        properties = "OSGI-INF/liferay/rest/v1/news.properties",
        scope = ServiceScope.PROTOTYPE, service = NewsResource.class
)
public class NewsResourceImpl extends BaseNewsResourceImpl {

    private PortalCache<String, List<NewsEntry>> _structuredContentCache;

    @Reference
    private PortalCacheManager<String, List<NewsEntry>> _portalCacheManager;

    @Activate
    public void activate() {
        // Initialize cache with a unique name and TTL (5 minutes = 300 seconds)
        _structuredContentCache = _portalCacheManager.getPortalCache("news.rest.cache");
    }

    @Override
    public Page<NewsEntry> getNews(Integer folderId) throws Exception {
        if (folderId == null) {
            throw new IllegalArgumentException("folderId cannot be null");
        }


        Locale preferredLocale = contextAcceptLanguage.getPreferredLocale();
        // set cache key
        String cacheKey = folderId + "_" + preferredLocale.toLanguageTag();


        // Check cache first
        List<NewsEntry> cachedEntries = _structuredContentCache.get(cacheKey);
        if (cachedEntries != null) {
            _log.info("Cache HIT for key: " + cacheKey);
            return Page.of(cachedEntries);
        }
        _log.info("Cache MISS for key: " + cacheKey);

        Long groupId = JournalFolderLocalServiceUtil.getFolder(folderId).getGroupId();

        List<JournalArticle> journalArticles = JournalArticleLocalServiceUtil.getArticles(groupId, folderId)
                .stream()
                .map(article -> {
                    try {
                        return JournalArticleLocalServiceUtil.getLatestArticle(article.getResourcePrimKey());
                    } catch (PortalException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());


        List<NewsEntry> newsEntries = journalArticles.stream()
                .map(journalArticle -> {
                    NewsEntry newsEntry = new NewsEntry();
                    SAXReaderUtil sax = new SAXReaderUtil();
                    Document doc;
                    try {
                        doc = sax.read(journalArticle.getContent());
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                    Node rootEle = doc.getRootElement();
                    Node titleNode = rootEle.selectSingleNode("/root/dynamic-element[@name='Text38176546']/dynamic-content");
                    Node dateNode = rootEle.selectSingleNode("/root/dynamic-element[@name='Date06600681']/dynamic-content");
                    Node imageNode = rootEle.selectSingleNode("/root/dynamic-element[@name='Image12152576']/dynamic-content");
                    Node descriptionNode = rootEle.selectSingleNode("/root/dynamic-element[@name='Text45263915']/dynamic-content");


                    newsEntry.setTitle(titleNode.getText()); // Set title
                    newsEntry.setDescription(descriptionNode.getText());
                    newsEntry.setDate(parseDate(dateNode.getText(), journalArticle));
                    newsEntry.setImage(parseImageUrl(imageNode.getText()));

                    return newsEntry;
                })
                .sorted(Comparator.comparing(NewsEntry::getDate))
                .collect(Collectors.toList());

        // Store in cache with 5 minutes
        _structuredContentCache.put(cacheKey, newsEntries, 300);
        _log.info("Cached data for key: " + cacheKey);


        return Page.of(newsEntries);
    }


    private Date parseDate(String dateStr, JournalArticle journalArticle) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return df.parse(dateStr);
        } catch (Exception e) {
            _log.error("Error parsing date for article ID " + journalArticle.getId(), e);
            return null;
        }
    }

    private String parseImageUrl(String jsonStr) {
        try {
            JSONObject json = JSONFactoryUtil.createJSONObject(jsonStr);
            return json.getString("url");
        } catch (JSONException e) {
            _log.error("Invalid JSON for image data: " + jsonStr, e);
            return "/o/headless-delivery/v1/images/placeholder.png";
        }
    }

    private static final Log _log = LogFactoryUtil.getLog(NewsResourceImpl.class);
}