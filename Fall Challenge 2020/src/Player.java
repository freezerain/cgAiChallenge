import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

class Player {

    public static void main(String args[]) throws InterruptedException, IOException {
        Reader r = new Reader();
        Inventory inv = new Inventory();
        Action[] actionList = new Action[1];
        while (true) {
            long startTime = System.currentTimeMillis();
            actionList = readAction(r, actionList, inv);
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

    static void readAndParseInput(BufferedReader reader, Action[] actionList, Inventory inv) throws IOException {
        int actionCount = Integer.parseInt(reader.readLine());
        actionList = new Action[actionCount];
        for (int i = 0; i < actionCount; i++){
            String[] value = reader.readLine().split(" ");
            if (value[1].equals("OPPONENT_CAST")) continue;
            Action a = new Action(Integer.parseInt(value[0]), ActionType.valueOf(value[1]),
                                  new int[]{Integer.parseInt(value[2]), Integer.parseInt(
                                          value[3]), Integer.parseInt(value[4]), Integer.parseInt(
                                          value[5])}, Integer.parseInt(value[6]),
                                  Integer.parseInt(value[7]), Integer.parseInt(value[8]),
                                  Integer.parseInt(value[9]) > 0, Integer.parseInt(value[10]) > 0);
            actionList[i] = a;
        }
        //System.err.println("ActionList: " + actionList);
        for (int i = 0; i < 1; i++){//TODO less then 2 to get second player
            String[] value = reader.readLine().split(" ");
            for (int j = 0; j < 4; j++){
                inv.items[j] = Integer.parseInt(value[0]);
            }
            inv.score = Integer.parseInt(value[4]);
        }
        reader.readLine();
    }

    static Action[] readAction(Reader r, Action[] actionList, Inventory inv) throws IOException {
        int actionCount = r.nextInt();
        actionList = new Action[actionCount];
        for (int i = 0; i < actionCount; i++){
            Action a = new Action(r.nextInt(), ActionType.valueOf(r.readString()),
                                  new int[]{r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt()},
                                  r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt() > 0,
                                  r.nextInt() > 0);
            if (a.type == ActionType.OPPONENT_CAST) continue;
            actionList[i] = a;
        }
        for (int i = 0; i < 1; i++){//TODO less then 2 to get second player
            for (int j = 0; j < 4; j++)
                inv.items[j] = r.nextInt();
            inv.score = r.nextInt();
        }
        r.readLine();
        System.err.println("readAction list: " + Arrays.toString(actionList));
        return actionList;
    }

    static void readInput(BufferedReader r, Action[] actionList, Inventory inv) throws IOException {
        StringTokenizer st = new StringTokenizer(r.readLine());
        int actionCount = Integer.parseInt(st.nextToken());
        actionList = new Action[actionCount];
        for (int i = 0; i < actionCount; i++){
            st = new StringTokenizer(r.readLine());
            Action a = new Action(Integer.parseInt(st.nextToken()),
                                  ActionType.valueOf(st.nextToken()),
                                  new int[]{Integer.parseInt(st.nextToken()), Integer.parseInt(
                                          st.nextToken()), Integer.parseInt(
                                          st.nextToken()), Integer.parseInt(st.nextToken())},
                                  Integer.parseInt(st.nextToken()),
                                  Integer.parseInt(st.nextToken()),
                                  Integer.parseInt(st.nextToken()),
                                  Integer.parseInt(st.nextToken()) > 0,
                                  Integer.parseInt(st.nextToken()) > 0);
            if (a.type == ActionType.OPPONENT_CAST) continue;
            actionList[i] = a;
        }
        //System.err.println("ActionList: " + actionList);
        for (int i = 0; i < 1; i++){//TODO less then 2 to get second player
            st = new StringTokenizer(r.readLine());
            for (int j = 0; j < 4; j++){
                inv.items[j] = Integer.parseInt(st.nextToken());
            }
            inv.score = Integer.parseInt(st.nextToken());
        }
        st = new StringTokenizer(r.readLine());
        //st = new StringTokenizer(r.readLine());
        //r.readLine();
        //r.readLine();
    }


    static Action valueActions(Inventory inv, Action[] actionList) {
        System.err.println("Value action list:" + Arrays.toString(actionList));
        double[] resValue = valueResources(inv , actionList);
        Action bestAction = null;
        int steps = 0;
        double actionValue = 0;
        for (Action action: actionList){
            if(action == null || action.type == ActionType.LEARN) continue;
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


    static double[] valueResources(Inventory inv, Action[] actionList) {
        double[] resourceValue = new double[]{0.0, 1.0, 1.0, 1.0};
        for (Action action: actionList){
            System.err.println("Value res action : "  + action);
            if (action == null ||action.type != ActionType.BREW) continue;
            int[] value = inv.getMissingItems(action.delta);
            double sum = Math.abs(Arrays.stream(value).sum());
            System.err.println(
                    "missing items for id: " + action.id + " : " + Arrays.toString(value) +
                    " sum: " + sum);
            System.err.println("Action price: " + action.price);
            for (int j = 0; j < 4; j++){
                if (value[j] < 0) {
                    System.err.println(
                            "Setting value in resource i: " + j + " value: " + action.price / sum +
                            " max: " + Math.max(resourceValue[j], action.price / sum));
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
        BREW, REST, CAST, OPPONENT_CAST, LEARN, WAIT, NONE
    }

    static class Reader {
        final private int BUFFER_SIZE = 1 << 16;
        private DataInputStream din;
        private byte[] buffer;
        private int bufferPointer, bytesRead;

        public Reader() {
            din = new DataInputStream(System.in);
            buffer = new byte[BUFFER_SIZE];
            bufferPointer = bytesRead = 0;
        }

        public Reader(String file_name) throws IOException {
            din = new DataInputStream(new FileInputStream(file_name));
            buffer = new byte[BUFFER_SIZE];
            bufferPointer = bytesRead = 0;
        }

        public String readString() throws IOException {
            byte[] buf = new byte[64]; // line length
            int cnt = 0, c;
            while ((c = read()) != -1) {
                if (c == '\n' || c == ' ') {
                    if (cnt != 0) break;
                    else continue;
                }
                buf[cnt++] = (byte) c;
            }
            return new String(buf, 0, cnt);
        }

        public String readLine() throws IOException {
            byte[] buf = new byte[64]; // line length
            int cnt = 0, c;
            while ((c = read()) != -1) {
                if (c == '\n') {
                    if (cnt != 0) break;
                    else continue;
                }
                buf[cnt++] = (byte) c;
            }
            return new String(buf, 0, cnt);
        }

        public int nextInt() throws IOException {
            int ret = 0;
            byte c = read();
            while (c <= ' ') c = read();
            boolean neg = (c == '-');
            if (neg) c = read();
            do ret = ret * 10 + c - '0'; while ((c = read()) >= '0' && c <= '9');
            if (neg) return -ret;
            return ret;
        }

        private void fillBuffer() throws IOException {
            bytesRead = din.read(buffer, bufferPointer = 0, BUFFER_SIZE);
            if (bytesRead == -1) buffer[0] = -1;
        }

        private byte read() throws IOException {
            if (bufferPointer == bytesRead) fillBuffer();
            return buffer[bufferPointer++];
        }

        public void close() throws IOException {
            if (din == null) return;
            din.close();
        }
    }
}