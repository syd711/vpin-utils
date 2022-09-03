package de.mephisto.vpin.highscores;

import de.mephisto.vpin.games.GameInfo;
import de.mephisto.vpin.games.GameRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HighscoreResolverTest {
  private final static Logger LOG = LoggerFactory.getLogger(HighscoreResolverTest.class);

  @Test
  public void testHighscoreResolver() throws Exception {
    HighscoreResolver highscoreResolver = new HighscoreResolver();
    highscoreResolver.refresh();

    GameRepository repository = GameRepository.create();
    List<GameInfo> games = repository.getGameInfos();
    List<GameInfo> valid = new ArrayList<>();
    for (GameInfo game : games) {
      if (game.getHighscore() != null) {
        assertFalse(game.getHighscore().getScores().isEmpty());
        assertFalse(game.getHighscore().getUserInitials().isEmpty());
        valid.add(game);
      }
    }

    valid.sort((o1, o2) -> (int) (o1.getLastPlayed().getTime() - o2.getLastPlayed().getTime()));


    LOG.info("************************************************************************************");
    LOG.info("Finished highscore reading, " + valid.stream());
    LOG.info("************************************************************************************");
    for (GameInfo gameInfo : valid) {
      LOG.info(gameInfo.getLastPlayed() + ": " + gameInfo.getGameDisplayName());
    }
  }


  @Test
  public void testHighscore() {
    HighscoreResolver highscoreResolver = new HighscoreResolver();
    highscoreResolver.refresh();

    GameRepository gameRepository = GameRepository.create();
    GameInfo game = gameRepository.getGameByRom("hpgof");
    assertNotNull(game.getHighscore());
    assertNotNull(game.getHighscore().getUserInitials());
    assertFalse(game.getHighscore().getScores().isEmpty());
    LOG.info(game.getHighscore().getRaw());
  }

}
