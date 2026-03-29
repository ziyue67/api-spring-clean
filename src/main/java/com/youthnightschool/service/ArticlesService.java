package com.youthnightschool.service;

import com.youthnightschool.dto.UpsertArticleRequest;
import com.youthnightschool.entity.Article;
import com.youthnightschool.repository.ArticleRepository;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WeChat official account article sync.
 * Mirrors the NestJS ArticlesService exactly, including URL whitelisting
 * and title sanitization.
 */
@Service
public class ArticlesService {

  private static final Set<String> ALLOWED_HOSTS = Set.of(
      "mp.weixin.qq.com",
      "www.huvtc.edu.cn",
      "huvtc.edu.cn"
  );

  private final ArticleRepository articleRepository;

  public ArticlesService(ArticleRepository articleRepository) {
    this.articleRepository = articleRepository;
  }

  // --- input sanitization ---

  private String normalizeTextInput(Object value) {
    if (value instanceof String s) {
      return s;
    }
    if (value instanceof Number || value instanceof Boolean) {
      return String.valueOf(value);
    }
    return "";
  }

  private String sanitizeArticleTitle(Object title) {
    String normalized = normalizeTextInput(title)
        .replaceAll("\\p{Cc}", " ")
        .replace("<", "").replace(">", "").replace("'", "")
        .replace("\"", "").replace("`", "")
        .replaceAll("\\s+", " ")
        .trim();
    return truncated(normalized, 255);
  }

  private String normalizeArticleLink(Object link) {
    String normalized = normalizeTextInput(link).trim();
    if (normalized.isEmpty()) {
      return "";
    }
    try {
      URI uri = new URI(normalized);
      if (!"https".equalsIgnoreCase(uri.getScheme())) {
        return "";
      }
      if (!ALLOWED_HOSTS.contains(uri.getHost())) {
        return "";
      }
      return truncated(normalized, 500);
    } catch (Exception e) {
      return "";
    }
  }

  private String truncated(String s, int max) {
    return s.length() > max ? s.substring(0, max) : s;
  }

  // --- public API ---

  /**
   * Lists articles ordered by publishTime desc, createdAt desc.
   */
  public Map<String, Object> listAll(Integer limit) {
    int normalizedLimit = Math.min(Math.max(limit != null ? limit : 20, 1), 100);
    List<Article> articles = articleRepository
        .findAllByOrderByPublishTimeDescCreateTimeDesc(PageRequest.of(0, normalizedLimit))
        .getContent();

    List<Map<String, Object>> data = articles.stream()
        .map(this::mapArticle)
        .toList();

    return Map.of("success", true, "data", data);
  }

  /**
   * Lists recent articles (max 10).
   */
  public Map<String, Object> listRecent(Integer limit) {
    int effectiveLimit = Math.min(limit != null ? limit : 3, 10);
    Map<String, Object> result = listAll(effectiveLimit);
    return Map.of("success", true, "data", result.get("data"));
  }

  /**
   * Upserts a batch of articles. URL-whitelists and sanitizes title/link.
   */
  @Transactional
  public Map<String, Object> upsertMany(List<UpsertArticleRequest> payload) {
    List<UpsertArticleRequest> normalized = payload.stream()
        .map(item -> new UpsertArticleRequest(
            item.legacyId(),
            sanitizeArticleTitle(item.title()),
            normalizeArticleLink(item.link()),
            item.publishTime()
        ))
        .filter(item -> item.title() != null && !item.title().isEmpty()
            && item.link() != null && !item.link().isEmpty())
        .toList();

    for (UpsertArticleRequest article : normalized) {
      articleRepository.findByLink(article.link()).ifPresentOrElse(
          existing -> {
            existing.setTitle(article.title());
            existing.setPublishTime(article.publishTime());
            articleRepository.save(existing);
          },
          () -> {
            Article newArticle = new Article();
            newArticle.setTitle(article.title());
            newArticle.setLink(article.link());
            newArticle.setPublishTime(article.publishTime());
            articleRepository.save(newArticle);
          }
      );
    }

    return Map.of(
        "success", true,
        "count", normalized.size(),
        "message", "成功同步 " + normalized.size() + " 篇文章"
    );
  }

  private Map<String, Object> mapArticle(Article article) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", article.getId());
    map.put("title", sanitizeArticleTitle(article.getTitle()));
    map.put("link", normalizeArticleLink(article.getLink()));
    map.put("publish_time", article.getPublishTime());
    map.put("create_time", article.getCreateTime());
    return map;
  }
}
