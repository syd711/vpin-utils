package de.mephisto.vpin.highscores;

import de.mephisto.vpin.GameInfo;
import de.mephisto.vpin.VPinService;
import de.mephisto.vpin.util.SystemInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class HighscoreManager {
  private final static Logger LOG = LoggerFactory.getLogger(HighscoreManager.class);

  private final Map<Integer, Highscore> cache = new HashMap<>();
  private final HighscoreResolver highscoreResolver;

  private final HighscoreFilesWatcher highscoreWatcher;
  private final VPinService VPinService;

  private final List<HighscoreChangeListener> listeners = new ArrayList<>();

  public HighscoreManager(VPinService service) {
    this.VPinService = service;

    this.highscoreResolver = new HighscoreResolver();


    this.highscoreWatcher = new HighscoreFilesWatcher(service, this);
    this.highscoreWatcher.start();
  }

  public void addHighscoreChangeListener(HighscoreChangeListener listener) {
    this.listeners.add(listener);
  }

  public void removeHighscoreChangeListener(HighscoreChangeListener listener) {
    this.listeners.remove(listener);
  }

  public void destroy() {
    this.highscoreWatcher.setRunning(false);
  }

  public Highscore getHighscore(GameInfo game) {
    if (StringUtils.isEmpty(game.getRom())) {
      return null;
    }

    if (!cache.containsKey(game.getId())) {
      Highscore highscore = highscoreResolver.loadHighscore(game);
      cache.put(game.getId(), highscore);
    }

    return cache.get(game.getId());
  }

  public void invalidateHighscore(GameInfo game) {
    highscoreResolver.refresh();
    cache.remove(game.getId());
    LOG.info("Invalidated cached highscore of " + game);
  }

  public void notifyHighscoreChange(HighscoreChangedEvent event) {
    new Thread(() -> {
      try {
        String name = Thread.currentThread().getName();
        Thread.currentThread().setName("Highscore Update [" + name + "]");
        invalidateHighscore(event.getGameInfo());
        for (HighscoreChangeListener listener : listeners) {
          listener.highscoreChanged(event);
        }
      } catch (Exception e) {
        LOG.error("Failed to trigger highscore updates: " + e.getMessage(), e);
      }
    }).start();
  }
}
