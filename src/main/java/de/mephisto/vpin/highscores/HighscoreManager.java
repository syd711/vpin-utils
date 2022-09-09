package de.mephisto.vpin.highscores;

import de.mephisto.vpin.games.GameInfo;
import de.mephisto.vpin.games.GameRepository;
import de.mephisto.vpin.util.SystemInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HighscoreManager {
  private final static Logger LOG = LoggerFactory.getLogger(HighscoreManager.class);

  private final Map<Integer, Highscore> cache = new HashMap<>();
  private HighscoreResolver highscoreResolver;

  private final HighscoreFilesWatcher highscoreWatcher;
  private GameRepository gameRepository;

  public HighscoreManager(GameRepository gameRepository) {
    this.gameRepository = gameRepository;

    this.highscoreResolver = new HighscoreResolver();
    this.highscoreResolver.refresh();

    SystemInfo info = SystemInfo.getInstance();
    List<File> watching = Arrays.asList(info.getNvramFolder(), info.getVPRegFile().getParentFile());
    this.highscoreWatcher = new HighscoreFilesWatcher(gameRepository, this, watching);
    this.highscoreWatcher.start();
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
    cache.remove(game.getId());
    LOG.info("Invalidated cached highscore of " + game);
  }

  public void refreshHighscores() {
    this.highscoreResolver.refresh();
  }
}
