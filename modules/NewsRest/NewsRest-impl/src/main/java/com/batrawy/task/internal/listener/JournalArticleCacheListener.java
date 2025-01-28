package com.batrawy.task.internal.listener;

import com.batrawy.task.dto.v1.NewsEntry;
import com.batrawy.task.internal.resource.v1.NewsResourceImpl;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.cache.PortalCacheManager;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Set;


@Component(
        immediate = true,
        service = ModelListener.class
)

public class JournalArticleCacheListener extends BaseModelListener<JournalArticle> {

    @Override
    public void onAfterCreate(JournalArticle journalArticle) {
        _log.info("Listener: onAfterCreate triggered for article ID " + journalArticle.getId());
        clearCache(journalArticle);
    }

    @Override
    public void onAfterUpdate(JournalArticle journalArticle, JournalArticle updatedArticle) {
        super.onAfterUpdate(journalArticle, updatedArticle);
        _log.info("Listener: onAfterUpdate triggered for article ID " + journalArticle.getId());
        clearCache(journalArticle);
    }

    private void clearCache(JournalArticle journalArticle) {
        long folderId = journalArticle.getFolderId();

        // Retrieve the cache instance
        PortalCache<String, List<NewsEntry>> cache = _portalCacheManager.getPortalCache("news.rest.cache");

        // Get all cache keys associated with this folderId
        Set<String> cacheKeys = NewsResourceImpl.getFolderCacheKeys(folderId);
        if (cacheKeys != null && !cacheKeys.isEmpty()) {
            for (String cacheKey : cacheKeys) {
                cache.remove(cacheKey);
            }
            // Remove the folder's cache keys tracking entry
            NewsResourceImpl.removeFolderCacheKeys(folderId);
        }
    }

    @Reference(target = "(portal.cache.manager.name=com.liferay.portal.kernel.cache.PortalCacheManager)")
    private PortalCacheManager<String, List<NewsEntry>> _portalCacheManager;

    private static final Log _log = LogFactoryUtil.getLog(NewsResourceImpl.class);

}
