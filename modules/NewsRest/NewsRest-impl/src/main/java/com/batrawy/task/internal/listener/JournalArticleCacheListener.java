
package com.batrawy.task.internal.listener;

import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.cache.PortalCacheManager;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        service = ModelListener.class
)
public class JournalArticleCacheListener extends BaseModelListener<JournalArticle> {

    private static final Log _log = LogFactoryUtil.getLog(JournalArticleCacheListener.class);
    private static final String CACHE_NAME = "news.rest.cache";

    // Inject the PortalCacheManager service
    @Reference
    private PortalCacheManager<String, Object> _portalCacheManager;

    @Override
    public void onAfterCreate(JournalArticle journalArticle) {
        clearCache();
    }

    @Override
    public void onAfterUpdate(JournalArticle journalArticle, JournalArticle updatedArticle) {
        clearCache();
    }

    @Override
    public void onAfterRemove(JournalArticle journalArticle) {
        clearCache();
    }

    /**
     * Clears the cache used by the News REST module.
     */
    private void clearCache() {
        try {
            PortalCache<String, ?> cache = _portalCacheManager.getPortalCache(CACHE_NAME);
            if (cache != null) {
                cache.removeAll();
                _log.info("News REST cache cleared successfully.");
            }
            else {
                _log.warn("Cache with name " + CACHE_NAME + " not found.");
            }
        }
        catch (Exception e) {
            _log.error("Error clearing cache " + CACHE_NAME, e);
        }
    }
}