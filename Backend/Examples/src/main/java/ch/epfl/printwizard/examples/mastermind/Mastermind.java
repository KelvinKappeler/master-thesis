package ch.epfl.printwizard.examples.mastermind;

import java.util.Scanner;

class Mastermind {

	private static Scanner scanner = new Scanner(System.in);

	/**
	 * Lancement du Master-Mind(tm).
	 * @param args Inutilisé dans ce programme
	 */
	public static void main(String[] args) {
		mastermind(4, 6, 10);
	}

	/**
	 * Tirage d'un entier au hasard entre 1 et max.
	 * @param max La valeur maximale pouvant être retournée
	 * @return Un entire aléatoire entre 1 et max
	 */
	static int hasard(int max) {
		return (1 + (int) (Math.random() * max));
	}

	/**
	 * Tire une combinaison à deviner. 
	 * C'est la référence du tableau qui est passée
	 * en paramètre. La méthode tirerCombinaison pourra directement
	 * modifier la combinaison en mémoire.
	 * Ici, on suppose que n < combinaison.length.
	 * @param combinaison Référence vers le tableau à modifier
	 * @param n Longueur de la combinaison.
	 * @param m Valeur maximale à tirer aléatoirement
	 */ 
	static void tirerCombinaison(int[] combinaison, int n, int m) {
		for (int i = 0; i < n; i++) {
			combinaison [i] = hasard(m);
			}
	}

	/**
	 * Permet de lire la combinaison proposée
	 * par le joueur.
	 * @param combinaison Tableau dans lequel stocker les valeurs entrées par le
	 * joueur
	 * @param n Longueur de la combinaison
	 */ 
	static void demanderCoup(int[] combinaison, int n) {
		System.out.print("Entrez les ");
		System.out.print(n);
		System.out.print(" chiffres de votre proposition ");
		System.out.println("(terminés par un retour chariot) :");
		for (int i = 0; i < n; i++) {
			combinaison[i] = scanner.nextInt();
		}
	}

	/**
	 * Permet de comparer la combinaison à deviner avec la 
	 * combinaison proposée par le joueur.
	 * Dans reponse[0] sera stocké le nombre d'éléments bien devinés 
	 * et correctement placés.
	 * Dans reponse[1] sera stocké le nombre d'éléments bien devinés
	 * mais mal placés.
	 * @param n Longueur de la combinaison
	 * @param combinaison1 Combinaison proposée par le joueur
	 * @param combinaison2 Combinaison à deviner
	 * @param reponse Tableau de deux int contenant le nombre de bien placés
	 * (index 0) et le nombre de mal placés (index 1)
	 * @return true si la bonne combinaison est trouvée et false sinon
	 */ 
	static boolean compare(int n, int[] combinaison1, int[] combinaison2,
				int[] reponse) {
		// nombre de bien placés
		int nbOk = 0;
		// nombre de mal placés
		int nbCol = 0;
		boolean [] marque = new boolean[n];
		boolean trouve = true;
		// cette première boucle sert à trouver 
		// les éléments bien devinés et correctement placés.
		// Le tableau marque permet de marquer de tels
		// éléments pour qu'ils ne soient pas considérés
		// plusieurs fois.
		for (int i = 0; i < n; i++) {
			if (combinaison1 [i] == combinaison2 [i]) {
				nbOk++;
				marque[i] = true;
			} else {
				trouve = false;
				marque[i] = false;
			}
		}
		// la deuxième boucle suivante sert à identifier les
		// éléments bien devinés mais mal placés.
		for (int i = 0; i < n; i++) {
			if (combinaison1[i] != combinaison2[i]) {
				int j = 0;
				boolean trouveMalPlace = false;
				while ((j < n) && !trouveMalPlace) {
					if (!marque[j] && (combinaison1[i] == combinaison2[j])) {
						nbCol++;
						marque[j] = true;
						trouveMalPlace = true;
					}
					j++;
				}
			}
		}

		reponse[0] = nbOk;
		reponse[1] = nbCol;
		return trouve;
	}

	/**
	 * Affichage d'une combinaison.
	 * @param combinaison Combinaison à afficher
	 * @param n Longueur de la combinaison
	 */ 
	static void afficheCombinaison(int[] combinaison, int n) {
		for (int i = 0; i < n; i++)
			System.out.print(combinaison[i]);
		System.out.println(" ");
	};

	/**
	 * Affichage des indications destinées au joueur.
	 * @param reponse Tableau de deux int contenant le nombre de bien placés
	 * (index 0) et le nombre de mal placés (index 1)
	 */ 
	static void afficheReponse(int[] reponse) {
		for (int i = 0; i < reponse[0]; i++)
			System.out.print('#');
		for (int i = 0; i < reponse[1]; i++)
			System.out.print('O');
		System.out.println();
	}

	/**
	 * Affichage du texte d'acceuil.
	 * @param n Longueur du code
	 * @param m Valeur maximale permise dans la combinaison
	 * @param maxCoups limite de coups autorisée
	 */ 
	static void bienvenue(int n, int m, int maxCoups) {
		System.out.print("Pouvez vous trouver ma combinaison de ");
		System.out.print(n);
		System.out.print(" chiffres [compris entre 1 et ");
		System.out.print(m);
		System.out.print(" avec répétitions possibles]\n en moins de ");
		System.out.print(maxCoups);
		System.out.println(" coups ?");
	}

	/**
	 * Jeu du Master-Mind.
	 * Le programme tire une combinaison au hasard.
	 * Le joueur cherche à la deviner et fait des propositions de 
	 * combinaisons.
	 * Le programme indique à chaque coup au joueur combien d'éléments
	 * sont bien devinés et correctement placé ou bien devinés mais
	 * mal placés.
	 * Le jour a droit à <maxCoups> tentatives.
	 * @param size Longueur du code à deviner
	 * @param maxDigit Valeur maximale pouvant se trouver dans le code
	 * @param maxCoups Limite de coups pour trouver la réponse
	 */ 
	static void mastermind(int size, int maxDigit, int maxCoups) {
		int[] solution = new int[size];
		int[] proposition = new int[size];
		int nbCoups = 0;
		boolean trouve = false;
		int[] reponse = new int[2];

		bienvenue(size, maxDigit, maxCoups);
		tirerCombinaison(solution, size, maxDigit);

		do {
			demanderCoup(proposition, size);
			nbCoups++;
			trouve = compare(size, solution, proposition, reponse);
			afficheReponse(reponse);
		} while (!trouve && (nbCoups < maxCoups));

		if (trouve) {
			System.out.print("Bravo ! Vous avez trouvé en ");
			System.out.print(nbCoups);
			System.out.println(" coups");
		} else {
			System.out.println("Désolé vous n'avez pas trouvé...");
			System.out.println("La bonne réponse était ");
			afficheCombinaison(solution, size);
			System.out.println(".");
		}
	}

}
