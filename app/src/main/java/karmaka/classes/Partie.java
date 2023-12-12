package karmaka.classes;

import java.util.ArrayList;

import karmaka.classes.piles.Deck;
import karmaka.classes.piles.Fosse;
import karmaka.classes.piles.Main;
import karmaka.classes.piles.Oeuvres;
import karmaka.classes.piles.Source;
import karmaka.classes.piles.VieFuture;
import karmaka.view.Router;

/**
 * La classe {@code Partie} représente le moteur de jeu principal pour Karmaka.
 * Elle gère le déroulement des tours, les actions des joueurs, et maintient l'état actuel de la partie.
 *
 * <p>Cette classe suit le modèle du singleton, garantissant une unique instance de la partie.</p>
 *
 * <p>Les différentes étapes du jeu sont représentées par l'énumération {@code Etape}.</p>
 */
public final class Partie {
    private static Partie instance = null;
    private GameData gameData = new GameData();

    /**
     * L'énumération {@code Etape} représente les différentes étapes du déroulement d'une partie de Karmaka.
     * Chaque valeur de cette énumération correspond à une phase spécifique du tour de jeu.
     */
    public enum Etape {
        DEBUT, PIOCHER_DECK, CHOISIR_CARTE_MAIN, CHOISIR_UTILISATION_CARTE, PROPOSER_CARTE,
        PROPOSER_CARTE_REJOUER, TOUR_SUIVANT, MEURT, MORT, GAGNANT
    }

    /**
     * Méthode d'initialisation d'une partie de Karmaka avec deux joueurs.
     *
     * @param j1 Le joueur 1.
     * @param j2 Le joueur 2.
     */
    private Partie(Joueur j1, Joueur j2) {
        gameData.joueurs[0] = j1;
        gameData.joueurs[1] = j2;
        distribuer();
        tour();
    }

    /**
     * Constructeur privé pour le singleton, initialise la partie sans joueurs.
     */
    private Partie() {
    }

    /**
     * Initialise la partie avec deux joueurs.
     *
     * @param joueur1 Le joueur 1.
     * @param joueur2 Le joueur 2.
     */
    public static void init(Joueur joueur1, Joueur joueur2) {
        if (instance == null) {
            instance = new Partie(joueur1, joueur2);
        }
    }

    /**
     * Initialise la partie sans joueurs.
     */
    public static void init() {
        if (instance == null) {
            instance = new Partie();
        }
    }

    public static Partie getInstance() {
        return instance;
    }

    public ArrayList<Action> getActionsPossibles() {
        return gameData.actionsPossibles;
    }

    public Joueur getJoueur(int i) {
        return gameData.joueurs[i];
    }

    public Source getSource() {
        return gameData.source;
    }

    public Fosse getFosse() {
        return gameData.fosse;
    }

    public int getTour() {
        return gameData.tour;
    }

    public Carte getCarteChoisie() {
        return gameData.carteChoisie;
    }

    public void setCarteChoisie(Carte carte) {
        gameData.carteChoisie = carte;
    }

    public void setEtape(Etape et) {
        gameData.etape = et;
    }

    /**
     * Distribue initialement les cartes aux joueurs en les piochant depuis la source.
     * Chaque joueur reçoit 4 cartes dans sa main et 2 cartes dans sa pile deck.
     */
    private void distribuer() {
        for (int i = 0; i < 2; i++) {
            gameData.joueurs[i].getMain().ajouter(gameData.source.piocher(4));
            gameData.joueurs[i].getDeck().ajouter(gameData.source.piocher(2));
        }
    }

    /**
     * Sauvegarde l'état actuel de la partie en utilisant le gestionnaire de routage.
     */
    public void sauvegarder() {
        Router.getInstance().sauvegarder(gameData);
    }

    /**
     * Charge l'état précédemment sauvegardé de la partie en utilisant le gestionnaire de routage.
     * Si une sauvegarde est chargée avec succès, la scène est définie sur "plateau".
     */
    public void charger() {
        GameData saveData = Router.getInstance().charger();
        if (saveData != null) {
            gameData = saveData;
            Router.getInstance().setScene("plateau");
            ;
        }
    }

    /**
     * Effectue une série d'actions et de vérifications pour gérer le déroulement d'un tour de jeu.
     */
    public void tour() {
        Joueur joueur = gameData.joueurs[gameData.tour];
        Deck deck = joueur.getDeck();
        Main main = joueur.getMain();
        Oeuvres oeuvres = joueur.getOeuvres();
        VieFuture vieFuture = joueur.getVieFuture();
        switch (gameData.etape) {
            case DEBUT:
                if (gameData.source.size() == 0) {
                    gameData.source.ajouter(gameData.fosse.piocher(gameData.fosse.size()));
                    gameData.source.melanger();
                    tour();
                    break;
                }
                if (joueur.isMort()) {
                    gameData.etape = Etape.MORT;
                    tour();
                    break;
                }
                if (deck.size() > 0) {
                    // Cas normal de jeu
                    joueur.afficher("Veuillez piocher une carte dans votre deck");
                    gameData.actionsPossibles.clear();
                    gameData.actionsPossibles.add(Action.PIOCHER_DECK);
                    gameData.etape = Etape.PIOCHER_DECK;
                    if (joueur.isRobot()) {
                        tour();
                    }
                } else {
                    if (main.size() > 0) {
                        // Le joueur n'a plus de carte dans son deck mais en a dans sa main donc il joue
                        gameData.etape = Etape.CHOISIR_CARTE_MAIN;
                        tour();
                    } else {
                        // Le joueur n'a plus rien donc il meuurt dans d'atroces souffrances !
                        gameData.etape = Etape.MEURT;
                        tour();
                    }
                }
                break;
            case MEURT:
                gameData.etape = Etape.TOUR_SUIVANT;
                joueur.setMort(true);
                joueur.afficher("Vous êtes mort, vous passez votre tour.");
                tour();
                break;
            case MORT:
                int echellekarmique = joueur.getEchelleKarmique();
                int nbAnneaux = joueur.getNbAnneaux();
                int points = oeuvres.calculerPoints();
                joueur.afficher(
                        "Vous etes mort. Nous allons vérifier si vous arrivez à vous réincarner.");

                // On ajuste nbAnneaux (et points) SSI cela peut changer l'issue Reussite/Echec
                // de la réincarnation
                if ((points + nbAnneaux) >= echellekarmique && points < echellekarmique) {
                    // En gros je veux que l'utilisateur choisisse si OUI ou NON il décide de
                    // dépenser le nb d'anneaux karmique nécessaire pour se réincarner
                    String choixAnneaux = joueur
                            .choix("Vous pouvez vous réincarner ! Il vous faut pour cela dépenser "
                                    + (echellekarmique - points)
                                    + " anneaux Karmiques. Vous en avez actuellement " + nbAnneaux
                                    + ", allez vous les utiliser ?", "Oui", "Non");
                    // Le joueur dépense le nombre d'anneaux nécessaire pour se réincarner et gagne
                    // ce meme montant en points
                    if (choixAnneaux == "Oui") {
                        nbAnneaux -= echellekarmique - points;
                        points += echellekarmique - points;
                    }
                }

                // Cas reussite + Victoire
                if (points >= echellekarmique && echellekarmique == 7) {
                    joueur.afficher(
                            "Vous avez enfin atteint la Transcendance ! Quelle belle aventure !");
                    gameData.gagnant = gameData.tour;
                    gameData.etape = Etape.GAGNANT;
                    tour();
                    break;
                }
                // Cas simple reussite
                else if (points >= echellekarmique) {
                    joueur.afficher(
                            "Félicitations, vous avez réussi à vous réincarner. Vous vous rapprochez de la Transcendance.");
                    joueur.setEchelleKarmique(echellekarmique + 1);
                    joueur.setNbAnneaux(nbAnneaux);
                }
                // Cas echec
                else {
                    joueur.setNbAnneaux(nbAnneaux + 1);
                    joueur.afficher(
                            "Vous n'avez pas réussi à vous réincarner, prenez un anneau karmique en compensation. Vous en avez maintenant "
                                    + joueur.getNbAnneaux() + ".");
                }
                gameData.fosse.ajouter(oeuvres.piocher(oeuvres.size()));
                // Supprimer les System.out.println si cette partie du programme marche
                System.out.println(main.size());
                System.out.println(vieFuture.size());
                main.ajouter(vieFuture.piocher(vieFuture.size()));
                System.out.println(main.size());
                System.out.println(vieFuture.size());
                if (main.size() < 6) {
                    joueur
                            .afficher("Vous avez moins de 6 cartes dans votre vie future. Vous allez piocher "
                                    + (6 - main.size()) + " cartes de la Source.");
                    deck.ajouter(gameData.source.piocher(6 - main.size()));
                }
                gameData.etape = Etape.TOUR_SUIVANT;
                joueur.setMort(false);
                tour();
                break;
            case PIOCHER_DECK:
                main.ajouter(deck.piocher());
                Router.getInstance().update();
                gameData.etape = Etape.CHOISIR_CARTE_MAIN;
                tour();
                break;
            case CHOISIR_CARTE_MAIN:
                joueur.afficher("Veuillez choisir une carte dans votre main" + (deck.size() > 0 ? " ou passer votre tour." : "."));
                gameData.actionsPossibles.clear();
                gameData.actionsPossibles.add(Action.CHOISIR_CARTE_MAIN);
                if (deck.size() > 0) {
                    gameData.actionsPossibles.add(Action.PASSER);
                }
                gameData.etape = Etape.CHOISIR_UTILISATION_CARTE;
                if (joueur.isRobot()) {
                    gameData.carteChoisie = joueur.choix("robot choisit carte main", main.getCartes());
                    tour();
                }
                break;
            case CHOISIR_UTILISATION_CARTE:
                String choix = joueur.choix(
                        "Veuillez choisir une utilisation pour la carte " + gameData.carteChoisie.getNom(),
                        "Points", "Pouvoir", "Futur");
                System.out.println(choix);
                if (choix == null) {
                    gameData.etape = Etape.CHOISIR_CARTE_MAIN;
                    tour();
                    break;
                }
                switch (choix) {
                    case "Points":
                        oeuvres.ajouter(main.piocher(gameData.carteChoisie));
                        gameData.etape = Etape.TOUR_SUIVANT;
                        tour();
                        break;
                    case "Pouvoir":
                        gameData.carteChoisie = main.piocher(gameData.carteChoisie);
                        gameData.carteChoisie.pouvoir();
                        gameData.etape = Etape.PROPOSER_CARTE;
                        tour();
                        break;
                    case "Futur":
                        vieFuture.ajouter(main.piocher(gameData.carteChoisie));
                        gameData.etape = Etape.TOUR_SUIVANT;
                        tour();
                        break;
                }
                Router.getInstance().update();
                break;
            case PROPOSER_CARTE:
                Router.getInstance().setScene("plateauPlaceholder");
                joueur
                        .afficher("Veuillez laisser votre adversaire choisir d'accepter ou non la carte.");
                String choixAdversaire = gameData.joueurs[(gameData.tour + 1) % 2].choix(
                        "Voulez vous accepter la carte " + gameData.carteChoisie.getNom(), "Accepter",
                        "Refuser");
                if (choixAdversaire == "Accepter") {
                    VieFuture vieFutureAdv = gameData.joueurs[(gameData.tour + 1) % 2]
                            .getVieFuture();
                    vieFutureAdv.ajouter(gameData.carteChoisie);
                } else {
                    gameData.fosse.ajouter(gameData.carteChoisie);
                }
                gameData.etape = Etape.TOUR_SUIVANT;
                tour();
                break;
            case PROPOSER_CARTE_REJOUER:
                Router.getInstance().setScene("plateauPlaceholder");
                joueur
                        .afficher("Veuillez laisser votre adversaire choisir d'accepter ou non la carte.");
                choixAdversaire = gameData.joueurs[(gameData.tour + 1) % 2].choix(
                        "Voulez vous accepter la carte " + gameData.carteChoisie.getNom(), "Accepter",
                        "Refuser");
                if (choixAdversaire == "Accepter") {
                    VieFuture vieFutureAdv = gameData.joueurs[(gameData.tour + 1) % 2]
                            .getVieFuture();
                    vieFutureAdv.ajouter(gameData.carteChoisie);
                } else {
                    gameData.fosse.ajouter(gameData.carteChoisie);
                }
                gameData.joueurs[(gameData.tour + 1) % 2]
                        .afficher("Le joueur peut rejouer une carte. Laissez-le continuer.");
                Router.getInstance().setScene("plateau");
                gameData.etape = Etape.CHOISIR_CARTE_MAIN;
                tour();
                break;
            case TOUR_SUIVANT:
                gameData.tour = (gameData.tour + 1) % 2;
                gameData.etape = Etape.DEBUT;
                gameData.actionsPossibles.clear();
                Router.getInstance().setScene("plateauPlaceholder");
                joueur.afficher("Changement de joueur ! Ne trichez pas !");
                Router.getInstance().setScene("plateau");
                tour();
                break;
            case GAGNANT:
                joueur
                        .afficher("Le joueur " + gameData.joueurs[(gameData.gagnant + 1)].getNom() + " a gagné !");
                break;
        }
        return;
    }
}
