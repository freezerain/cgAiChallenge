import java.util.*;
import java.io.*;
import java.math.*;
import java.util.stream.IntStream;

import static java.util.Comparator.reverseOrder;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        Inventory inv = new Inventory();
        // game loop
        while (true) {
            int actionCount = in.nextInt(); // the number of spells and recipes in play
            List<Action> actions = new ArrayList<>();
            for (int i = 0; i < actionCount; i++) {
                int actionId = in.nextInt(); // the unique ID of this spell or recipe
                String actionType = in.next(); // in the first league: BREW; later: CAST, OPPONENT_CAST, LEARN, BREW
                int delta0 = in.nextInt(); // tier-0 ingredient change
                int delta1 = in.nextInt(); // tier-1 ingredient change
                int delta2 = in.nextInt(); // tier-2 ingredient change
                int delta3 = in.nextInt(); // tier-3 ingredient change
                int price = in.nextInt(); // the price in rupees if this is a potion
                int tomeIndex = in.nextInt(); // in the first two leagues: always 0; later: the index in the tome if this is a tome spell, equal to the read-ahead tax; For brews, this is the value of the current urgency bonus
                int taxCount = in.nextInt(); // in the first two leagues: always 0; later: the amount of taxed tier-0 ingredients you gain from learning this spell; For brews, this is how many times you can still gain an urgency bonus
                boolean castable = in.nextInt() != 0; // in the first league: always 0; later: 1 if this is a castable player spell
                boolean repeatable = in.nextInt() != 0; // for the first two leagues: always 0; later: 1 if this is a repeatable player spell
                actions.add(new Action(actionId, new int[]{delta0, delta1, delta2, delta3},price));
            }
            for (int i = 0; i < 2; i++) {
                int inv0 = in.nextInt(); // tier-0 ingredients in inventory
                int inv1 = in.nextInt();
                int inv2 = in.nextInt();
                int inv3 = in.nextInt();
                int score = in.nextInt(); // amount of rupees
                inv.score = score;
                inv.items = new int[]{inv0, inv1, inv2,inv3};
            }
            if(in.hasNextLine()) in.nextLine();
            actions.sort(Comparator.comparingInt((Action a) -> a.price).reversed());
            Action bestAction = null;
            for (Action a : actions) {
                if(inv.isEnoughResources(a.delta)){
                    bestAction = a;
                    break;
                }
            }
            System.err.println(actions);
            if(bestAction == null) System.out.println("WAIT I am waiting!");
            else System.out.println("BREW " + bestAction.id + " I am brewing some shit! id: " + bestAction.id + " price: " + bestAction.price);
            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");


            // in the first league: BREW <id> | WAIT; later: BREW <id> | CAST <id> [<times>] | LEARN <id> | REST | WAIT
           // System.out.println("BREW 0");
        }
    }
    
    static class Inventory{
        int[] items;
        int score;

        public Inventory(int[] items, int score) {
            this.items = items;
            this.score = score;
        }
        public Inventory() {
            this.items = new int[4];
            this.score = 0;
        }
        boolean isEnoughResources(int[] delta) {
            boolean isPossible = IntStream.range(0, 4).map(i -> items[i] + delta[i]).allMatch(i -> i >=0);
            if(!isPossible) System.err.println(Arrays.toString(items) + " " +
                                               Arrays.toString(delta));
            return isPossible;
        }
    }
    
    static class Action{
        int id;
        String type;
        int[] delta;
        public int price;
        //tomeIndex
        //taxCount
        //castable
        //repeatable

        public Action(int id, int[] delta, int price) {
            this.id = id;
            this.delta = delta;
            this.price = price;
        }

        @Override
        public String toString() {
            return "Action{" + "id=" + id + ", price=" + price + '}';
        }
    }
}