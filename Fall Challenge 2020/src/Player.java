import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

class Player {

    public static void main(String args[]) throws InterruptedException {
        Scanner in = new Scanner(System.in);
        Inventory inv = new Inventory();
        while (true) {
            long startTime = System.currentTimeMillis();
            int actionCount = in.nextInt();
            List<Action> actionList = new ArrayList<>();
            List<Action> actionBrewList = new ArrayList<>();
            List<Action> actionCastList = new ArrayList<>();
            for (int i = 0; i < actionCount; i++){
                Action a = new Action(in.nextInt(), ActionType.valueOf(in.next()),
                                      new int[]{in.nextInt(), in.nextInt(), in.nextInt(),
                                              in.nextInt()},
                                      in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt() > 0,
                                      in.nextInt() > 0);
                if (a.type == ActionType.OPPONENT_CAST) continue;
                else if (a.type == ActionType.BREW) actionBrewList.add(a);
                else actionCastList.add(a);
                actionList.add(a);
            }
            //System.err.println("ActionList: " + actionList);
            for (int i = 0; i < 1; i++){//TODO less then 2 to get second player
                inv.items = new int[]{in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt()};
                inv.score = in.nextInt();
            }
            if (in.hasNextLine()) in.nextLine();//TODO skip second player
            if (in.hasNextLine()) in.nextLine();
            System.err.println("InitTime: " + (System.currentTimeMillis() - startTime) + " ms");
            /*
            long millis = 70 - (System.currentTimeMillis() - startTime);
            if(millis>0){
                sleep(millis);
                System.err.println("EndTime: " + (System.currentTimeMillis()-startTime) + " ms");
            }
            else System.err.println("Negative time: " + millis + " ms");
            System.out.println("WAIT");*/

            Action a = valueActions(inv, actionList);

            if (a == null) System.out.println("WAIT Waiting - null Action");
            else if (a.type == ActionType.BREW)
                System.out.println("BREW " + a.id + " Brewing id: " + a.id + " score: " + a.price);
            else if (a.type == ActionType.CAST) {
                if (a.castable) {
                    System.out.println("CAST " + a.id + " Casting id: " + a.id);
                } else {
                    System.out.println("REST Resting to cast id: " + a.id);
                }
            } else System.out.println("WAIT Waiting ActionType unknown");
            System.err.println("EndTime: " + (System.currentTimeMillis() - startTime) + " ms");
           /* Plan bestPlan = buildPlanRecursive(inv, actionBrewList, actionCastList, 2);

            if (bestPlan == null) {
                System.out.println("WAIT Waiting - dont have a plan");
                System.err.println("EndTime: " + (System.currentTimeMillis()-startTime) + " ms");
                continue;
            }
            Action firstAction = bestPlan.actionSequence.get(0);

            if (firstAction.type == ActionType.REST) System.out.println("REST Resting");
            else if (firstAction.type == ActionType.BREW) System.out.println(
                    "BREW " + firstAction.id + " Brewing id: " + firstAction.id + " score: " +
                    firstAction.price);
            else if (firstAction.type == ActionType.CAST)
                System.out.println("CAST " + firstAction.id + " Casting id: " + firstAction.id);
            else {
                System.out.println("WAIT Waiting - error");
                System.err.println(bestPlan.actionSequence);
            }
            System.err.println("EndTime: " + (System.currentTimeMillis()-startTime) + " ms");*/
        }
    }


    static Action valueActions(Inventory inv, List<Action> actionList) {
        double[] resValue = valueResources(inv, actionList);
        Action bestAction = null;
        int steps = 0;
        double actionValue = 0;
        for (Action action: actionList){
            if (inv.isEnoughResources(action.delta) &&
                ((!(action.type == ActionType.CAST)) || inv.isEnoughSpace(action))) {
                int newSteps = action.type == ActionType.BREW ? 1 : action.castable ? 2 : 3;
                double newValue = valueSpell(action, resValue);
                if (bestAction == null || actionValue / steps < newValue / newSteps) {
                    bestAction = action;
                    steps = newSteps;
                    actionValue = newValue;
                }
            }
        }
        System.err.println("Res value: " + Arrays.toString(resValue));
        System.err.println("");
        if (bestAction != null) System.err.println(
                "Action id: " + bestAction.id + " type: " + bestAction.type + " steps: " + steps +
                " value: " + actionValue);
        return bestAction;
    }

    static double valueSpell(Action a, double[] resValue) {
        if (a.type == ActionType.BREW) return a.price / 1.0;
        else {
            double value = 0.0;
            for (int i = 0; i < 4; i++){
                value += a.delta[i] * resValue[i];
            }
            return value;
        }
    }


    static double[] valueResources(Inventory inv, List<Action> actionList) {
        double[] resourceValue = new double[]{0.0, 1.0, 1.0, 1.0};
        for (Action action: actionList){
            if (action.type != ActionType.BREW) continue;
            int[] value = inv.getMissingItems(action.delta);
            double sum = Math.abs(Arrays.stream(value).sum());
            /*System.err.println(
                    "missing items for id: " + action.id + " : " + Arrays.toString(value) +
                    " sum: " + sum);*/
            //System.err.println("Action price: " + action.price);
            for (int j = 0; j < 4; j++){
                if (value[j] < 0) {
                    /*System.err.println(
                            "Setting value in resource i: " + j + " value: " + action.price / sum +
                            " max: " + Math.max(resourceValue[j], action.price / sum));*/
                    resourceValue[j] = Math.max(resourceValue[j], action.price / sum);
                }
            }
        }
        return resourceValue;
    }

    private static class Path {
        Action a;
        int steps;
    }

    static Plan buildPlanRecursive(Inventory inv, List<Action> brewList, List<Action> castList,
                                   int deepness) {
        if (deepness <= 0) return null;
        if (brewList.isEmpty()) return null;
        List<Action> notReachable = new ArrayList<>();
        Plan p = null;
        for (Action a: brewList){
            if (a.type == ActionType.BREW) {
                if (inv.isEnoughResources(a.delta)) {
                    Plan newPlan = new Plan(a);
                    if (p == null || newPlan.scorePerStep < newPlan.scorePerStep) p = newPlan;
                } else {
                    notReachable.add(a);
                }
            }
        }
        for (int i = 0; i < castList.size(); i++){
            if (!inv.isEnoughSpace(castList.get(i))) continue;
            if (!inv.isEnoughResources(castList.get(i).delta)) continue;
            List<Action> castCopyList = copyActionList(castList);
            Action cast = castCopyList.get(i);
            Plan castPrefixPlan;
            Plan newPlan = null;
            Inventory newInventory = new Inventory(inv);
            newInventory.changeInventory(cast);
            if (cast.castable) {
                cast.castable = false;
                castPrefixPlan = new Plan(cast);
            } else {
                castCopyList.forEach(a -> a.castable = true);
                castPrefixPlan = new Plan(new Action(ActionType.REST));
                castPrefixPlan.addStep(cast);
            }
            newPlan = buildPlanRecursive(newInventory, notReachable, castCopyList, deepness - 1);
            if (newPlan == null) continue;
            castPrefixPlan.appendAction(newPlan.actionSequence);
            if (p == null || p.scorePerStep < castPrefixPlan.scorePerStep) p = castPrefixPlan;
        }
        return p;
    }

    private static List<Action> copyActionList(List<Action> source) {
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

        public boolean isEnoughSpace(Action a) {
            int itemsCount = Arrays.stream(items).sum();
            int itemsDeltaCount = Arrays.stream(a.delta).sum();
            return itemsCount + itemsDeltaCount <= 10;
        }

        public int[] getMissingItems(int[] delta) {


            return IntStream.range(0, 4).map(i -> Math.min(delta[i] + items[i], 0)).toArray();
        }

        public void changeInventory(Action a) {
            IntStream.range(0, 4).forEach(i -> items[i] = items[i] + a.delta[i]);
        }

        boolean isEnoughResources(int[] delta) {
            for (int i = 0; i < 4; i++)
                if ((!(delta[i] > 0)) && (!(items[i] + delta[i] >= 0))) return false;
            return true;
        }
    }

    static class Action {
        int id;
        ActionType type;
        int[] delta;
        public int price;
        int tomeIndex;
        int taxCount;
        boolean castable;
        boolean repeatable;

        public Action(int id, ActionType type, int[] delta, int price, int tomeIndex,
                      int taxCount, boolean castable, boolean repeatable) {
            this.id = id;
            this.type = type;
            this.delta = delta;
            this.price = price;
            this.tomeIndex = tomeIndex;
            this.taxCount = taxCount;
            this.castable = castable;
            this.repeatable = repeatable;
        }

        public Action(Action a) {
            id = a.id;
            type = a.type;
            delta = a.delta.clone();
            price = a.price;
            tomeIndex = a.tomeIndex;
            taxCount = a.taxCount;
            castable = a.castable;
            repeatable = a.repeatable;
        }

        @Override
        public String toString() {
            return "Action{" + "id=" + id + ", type=" + type + ", delta=" + Arrays.toString(delta) +
                   ", price=" + price + ", tomeIndex=" + tomeIndex + ", taxCount=" + taxCount +
                   ", castable=" + castable + ", repeatable=" + repeatable + '}';
        }

        public Action(ActionType type) {
            this.type = type;
        }
    }

    enum ActionType {
        BREW, REST, CAST, OPPONENT_CAST, WAIT, NONE
    }
}