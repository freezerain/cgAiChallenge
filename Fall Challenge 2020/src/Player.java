import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        Inventory inv = new Inventory();
        // game loop
        while (true) {
            int actionCount = in.nextInt(); // the number of spells and recipes in play
            List<Action> actionBrewList = new ArrayList<>();
            List<Action> actionCastList = new ArrayList<>();
            for (int i = 0; i < actionCount; i++){
                int actionId = in.nextInt(); // the unique ID of this spell or recipe
                ActionType actionType = ActionType.valueOf(
                        in.next()); // in the first league: BREW; later: CAST, OPPONENT_CAST, 
                // LEARN, BREW
                int delta0 = in.nextInt(); // tier-0 ingredient change
                int delta1 = in.nextInt(); // tier-1 ingredient change
                int delta2 = in.nextInt(); // tier-2 ingredient change
                int delta3 = in.nextInt(); // tier-3 ingredient change
                int price = in.nextInt(); // the price in rupees if this is a potion
                int tomeIndex = in.nextInt(); // in the first two leagues: always 0; later: the 
                // index in the tome if this is a tome spell, equal to the read-ahead tax; For 
                // brews, this is the value of the current urgency bonus
                int taxCount = in.nextInt(); // in the first two leagues: always 0; later: the 
                // amount of taxed tier-0 ingredients you gain from learning this spell; For 
                // brews, this is how many times you can still gain an urgency bonus
                boolean castable = in.nextInt() !=
                                   0; // in the first league: always 0; later: 1 if this is a 
                // castable player spell
                boolean repeatable = in.nextInt() !=
                                     0; // for the first two leagues: always 0; later: 1 if this 
                // is a repeatable player spell
                Action a = new Action(actionId, actionType,
                                      new int[]{delta0, delta1, delta2, delta3}, price, castable);
                if (actionType == ActionType.BREW) actionBrewList.add(a);
                else actionCastList.add(a);
            }
            for (int i = 0; i < 2; i++){
                int inv0 = in.nextInt(); // tier-0 ingredients in inventory
                int inv1 = in.nextInt();
                int inv2 = in.nextInt();
                int inv3 = in.nextInt();
                int score = in.nextInt(); // amount of rupees
                inv.score = score;
                inv.items = new int[]{inv0, inv1, inv2, inv3};
            }
            if (in.hasNextLine()) in.nextLine();


            actionBrewList.sort(Comparator.comparingInt((Action a) -> a.price).reversed());
            Action bestAction = null;
            for (Action a: actionBrewList){
                if (inv.isEnoughResources(a.delta)) {
                    bestAction = a;
                    break;
                }
            }
            System.err.println(actionBrewList);
            if (bestAction == null) System.out.println("WAIT I am waiting!");
            else System.out.println(
                    "BREW " + bestAction.id + " I am brewing some shit! id: " + bestAction.id +
                    " price: " + bestAction.price);
            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");


            // in the first league: BREW <id> | WAIT; later: BREW <id> | CAST <id> [<times>] | 
            // LEARN <id> | REST | WAIT
            // System.out.println("BREW 0");
        }
    }

    static Plan buildPlanRecursive(Inventory inv, List<Action> brewList, List<Action> castList,
                                   int deepness) {
        if (deepness <= 0) return null;
        if (brewList.isEmpty()) return null;
        List<Action> notReacheble = new ArrayList<>();
        Plan p = null;
        for (Action a: brewList){
            if (a.type == ActionType.BREW) {
                if (inv.isEnoughResources(a.delta)) {
                    Plan newPlan = new Plan(a);
                    if (p == null || newPlan.scorePerStep < newPlan.scorePerStep) p = newPlan;
                } else {
                    notReacheble.add(a);
                }
            }
        }
        
        
        
        
        List<Action> castCopyList = copyActionList(castList);
        for (int i = 0; i < castList.size(); i++){
            Action cast = castList.get(i);
            Plan castPrefixPlan;
            Plan newPlan = null;
            if (cast.castable) {
                Action action = new Action(cast);
                action.castable = false;
                castPrefixPlan = new Plan(action);
                castCopyList.set(i, action);
            } else {
                castPrefixPlan = new Plan(new Action(ActionType.REST));
                castPrefixPlan.addStep(cast);
            }
            if(newPlan == null)continue;
            if(p == null || p.scorePerStep< newPlan.scorePerStep) p = newPlan;

        }
        
        
        
        
    }
    
    private static List<Action> copyActionList(List<Action> source){
        return source.stream().map(Action::new).collect(Collectors.toList());
    }

    static class Plan {
        int finalScore;
        int steps;
        double scorePerStep;
        List<Action> actionSequence = new ArrayList<>();

        public void appendAction(List<Action> actionList) {
            actionSequence.addAll(actionList);
            recalc();
        }

        public Plan(Action a) {
            addStep(a);
        }

        public void addStep(Action a) {
            actionSequence.add(a);
            recalc();
        }

        private void recalc() {
            Action action = actionSequence.get(actionSequence.size() - 1);
            finalScore = action.type == ActionType.BREW ? action.price : 0;
            steps = actionSequence.size();
            scorePerStep = (double) finalScore / steps;
        }
    }

    static class Inventory {
        int[] items = new int[4];
        int score = 0;

        public Inventory(Inventory inv) {
            items = inv.items.clone();
            this.score = inv.score;
        }

        public Inventory() {
        }

        boolean isEnoughResources(int[] delta) {
            boolean isPossible = IntStream.range(0, 4)
                    .map(i -> items[i] + delta[i])
                    .allMatch(i -> i >= 0);
            if (!isPossible)
                System.err.println(Arrays.toString(items) + " " + Arrays.toString(delta));
            return isPossible;
        }
    }

    static class Action {
        int id;
        ActionType type;
        int[] delta;
        public int price;
        //tomeIndex
        //taxCount
        boolean castable;
        //repeatable

        public Action(int id, ActionType type, int[] delta, int price, boolean castable) {
            this.id = id;
            this.type = type;
            this.delta = delta;
            this.price = price;
            this.castable = castable;
        }

        public Action(Action a) {
            id = a.id;
            type = a.type;
            delta = a.delta.clone();
            price = a.price;
            castable = a.castable;
        }

        public Action(ActionType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "Action{" + "id=" + id + ", price=" + price + '}';
        }
    }

    enum ActionType {
        BREW, REST, CAST, OPPONENT_CAST, WAIT, NONE
    }
}