package edu.wsu.eecs.gfc.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The major GFC mining algorithms.
 * <p>
 * @author Peng Lin penglin03@gmail.com
 */
public class RuleMiner<VT, ET> {

    private GraphDatabase<VT, ET> G;

    private double minSupp;

    private double minConf;

    private int maxSize;

    private int topK;

    private Map<Relation<VT, ET>, Integer> rOrder;

    private static final int DEFAULT_TOP_K_OF_PATTERNS = 50;

    private static final int DEFAULT_MAXIMUM_STREAM_LENGTH = 50000;

    private static final double DEFAULT_EPSILON = 0.10;

    private static final double INFINITESIMAL = 10E-8;

    public static class State<VT, ET> {

        private OGFCRule<VT, ET> phi;

        private Edge<VT, ET> f;

        private Set<Edge<VT, ET>> fSet;

        private List<Edge<VT, ET>> fList;

        private int fOrder;

        private double supp;

        private boolean isMaximal;

        public State(OGFCRule<VT, ET> phi, Edge<VT, ET> f) {
            this.phi = phi;
            this.f = f;
        }

        public State(OGFCRule<VT, ET> phi,
                     Set<Edge<VT, ET>> fSet,
                     List<Edge<VT, ET>> fList,
                     int fOrder,
                     double supp,
                     boolean isMaximal) {
            this.phi = phi;
            this.fSet = fSet;
            this.fList = fList;
            this.fOrder = fOrder;
            this.supp = supp;
            this.isMaximal = isMaximal;
        }
    }

    private RuleMiner(GraphDatabase<VT, ET> G, double minSupp, double minConf, int maxSize, Map<Relation<VT, ET>, Integer> rOrder, int topK) {
        this.G = G;
        this.minSupp = minSupp;
        this.minConf = minConf;
        this.maxSize = maxSize;
        this.rOrder = rOrder;
        this.topK = topK > 0 ? topK : DEFAULT_TOP_K_OF_PATTERNS;
    }

    public static <VT, ET> RuleMiner<VT, ET> createInit(GraphDatabase<VT, ET> G, double minSupp, double minConf, int maxSize, int topK) {
        Map<Relation<VT, ET>, Integer> rOrder = new HashMap<>();
        List<Relation<VT, ET>> rList = new ArrayList<>(G.relationSet());
        rList.sort((r1, r2) -> {
            int freq1 = G.getEdges(r1).size();
            int freq2 = G.getEdges(r2).size();
            return freq1 - freq2;
        });
        for (int i = 0; i < rList.size(); i++) {
            rOrder.put(rList.get(i), i);
        }
        return new RuleMiner<>(G, minSupp, minConf, maxSize, rOrder, topK);
    }

    private double getSupp(OGFCRule<VT, ET> phi, List<Edge<VT, ET>> examples) {
        int nr = 0;
        for (Edge<VT, ET> e : examples) {
            if (phi.matchSet().get(phi.x()).contains(e.srcNode())
                    && phi.matchSet().get(phi.y()).contains(e.dstNode())) {
                nr++;
            }
        }
        return ((double) nr / (double) examples.size());
    }

    private double getConf(OGFCRule<VT, ET> phi, int numOfExamples) {
        if (phi.supp < 0) {
            throw new RuntimeException("Confidence should be computed before the support.");
        }
        double npx = phi.matchSet().get(phi.x()).size();
        double npy = phi.matchSet().get(phi.y()).size();
        double cpxy = npx * npy;
        return phi.supp * (double) numOfExamples / cpxy;
    }

    private double getGTest(double fp, double fn, int numberOfPositiveExamples) {
        double p = fp < INFINITESIMAL ? INFINITESIMAL : fp > 1 - INFINITESIMAL ? 1 - INFINITESIMAL : fp;
        double n = fn < INFINITESIMAL ? INFINITESIMAL : fn > 1 - INFINITESIMAL ? 1 - INFINITESIMAL : fn;
        return Math.max(
                2 * numberOfPositiveExamples * (p * Math.log(p / INFINITESIMAL) + (1 - p) * Math.log(1 - p)),
                2 * numberOfPositiveExamples * Math.log(1 / (1 - n)));
    }


    private double getPCov(double gTest, double supp) {
        return (((2.0 / (1 + Math.exp(-2 * gTest))) - 1)) + supp * Math.sqrt(supp);
    }

    /**
     * The OGFC_batch algorithm.
     */
    public List<OGFCRule<VT, ET>> OGFC_batch(
            Relation<VT, ET> r,
            List<Edge<VT, ET>> positiveExamples,
            List<Edge<VT, ET>> negativeExamples
    ) {
        if (!G.relationSet().contains(r)) {
            System.out.println("Relation is not found. r = " + r);
            return new ArrayList<>();
        }

        List<OGFCRule<VT, ET>> phiList = new ArrayList<>();
        OGFCRule<VT, ET> root = OGFCRule.createInit(G, r, positiveExamples);

        Set<Edge<VT, ET>> rootFSet = root.searchExtensionEdges();
        List<Edge<VT, ET>> rootFList = new ArrayList<>(rootFSet);
        rootFList.sort(Comparator.comparingInt(e -> rOrder.get(Relation.fromEdge(e))));

        System.out.println("|F| = " + rootFList.size());

        State<VT, ET> rootState = new State<>(root, rootFSet, rootFList, Integer.MAX_VALUE, 1.0, true);
        List<State<VT, ET>> stack = new ArrayList<>(maxSize);
        stack.add(rootState);
        while (true) {
            if (phiList.size() >= DEFAULT_MAXIMUM_STREAM_LENGTH) {
                break;
            }

            if (stack.isEmpty()) {
                break;
            }

            if (stack.size() >= maxSize) {
                State<VT, ET> st = stack.remove(stack.size() - 1);
                phiList.add(st.phi);
            } else if (stack.get(stack.size() - 1).fList.isEmpty()) {
                State<VT, ET> st = stack.remove(stack.size() - 1);
                if (st.isMaximal) {
                    phiList.add(st.phi);
                }
            } else {
                State<VT, ET> curState = stack.get(stack.size() - 1);
                Edge<VT, ET> f = curState.fList.remove(curState.fList.size() - 1);
                Relation<VT, ET> rf = Relation.fromEdge(f);

                int fOrder = rOrder.get(rf);
                if (isPatternRepeated(stack, f, fOrder)) {
                    continue;
                }

                OGFCRule<VT, ET> phi2 = OGFCRule.extendEdge(curState.phi, f);
                phi2.supp = getSupp(phi2, positiveExamples);
                if (phi2.supp < minSupp) {
                    continue;
                }

                stack.get(stack.size() - 1).isMaximal = false;

                if (stack.size() == maxSize - 1) {
                    stack.add(new State<>(phi2, new HashSet<>(), new ArrayList<>(), fOrder, phi2.supp, true));
                } else {
                    Set<Edge<VT, ET>> fSet = phi2.searchExtensionEdges();
                    List<Edge<VT, ET>> fList = new ArrayList<>(fSet);
                    fList.sort(Comparator.comparingInt(e -> rOrder.get(Relation.fromEdge(e))));
                    stack.add(new State<>(phi2, fSet, fList, fOrder, phi2.supp, true));
                }
            }
        }

        for (OGFCRule<VT, ET> phi : phiList) {
            phi.conf = getConf(phi, positiveExamples.size());
            double fp = phi.supp;
            double fn = getSupp(phi, negativeExamples);
            phi.gTest = getGTest(fp, fn, positiveExamples.size());
            phi.pCov = getPCov(phi.gTest, phi.supp);
        }

        phiList = phiList.stream().filter(p -> p.conf > minConf).collect(Collectors.toList());

        return new ArrayList<>(phiList);
    }

    private boolean isPatternRepeated(List<State<VT, ET>> stack, Edge<VT, ET> f, int fOrder) {
        if (stack.size() <= 1) {
            return false;
        }
        State<VT, ET> stChild = stack.get(stack.size() - 1);
        State<VT, ET> stParent = stack.get(stack.size() - 2);
        return stParent.fSet.contains(f) && fOrder > stChild.fOrder;
    }

    /**
     * The OGFC_stream mining algorithm.
     */
    public List<OGFCRule<VT, ET>> OGFC_stream(
            Relation<VT, ET> r,
            List<Edge<VT, ET>> positiveExamples,
            List<Edge<VT, ET>> negativeExamples
    ) {
        if (!G.relationSet().contains(r)) {
            System.out.println("Relation is not found. r = " + r);
            return new ArrayList<>();
        }

        Deque<State<VT, ET>> stream = new ArrayDeque<>();

        double maxPCov = -1;

        OGFCRule<VT, ET> phi0 = OGFCRule.createInit(G, r, positiveExamples);

        List<Edge<VT, ET>> fList0 = new ArrayList<>(phi0.searchExtensionEdges());
        System.out.println("|F| = " + fList0.size());

        fList0.sort(Comparator.comparingInt(e -> rOrder.get(Relation.fromEdge(e))));

        System.out.println("Computing maxpcov value....");
        for (Edge<VT, ET> f0 : fList0) {

            OGFCRule<VT, ET> phi1 = OGFCRule.extendEdge(phi0, f0);

            phi1.supp = getSupp(phi1, positiveExamples);
            if (phi1.supp < minSupp) {
                continue;
            }

            phi1.conf = getConf(phi1, positiveExamples.size());
            if (phi1.conf < minConf) {
                continue;
            }

            double fp1 = phi1.supp;
            double fn1 = getSupp(phi1, negativeExamples);

            phi1.gTest = getGTest(fp1, fn1, positiveExamples.size());

            phi1.pCov = getPCov(phi1.gTest, phi1.supp);

            if (maxPCov < phi1.pCov) {
                maxPCov = phi1.pCov;
            }

            List<Edge<VT, ET>> fList = new ArrayList<>();
            for (Edge<VT, ET> f1 : phi1.searchExtensionEdges()) {
                if (rOrder.get(Relation.fromEdge(f1)) > rOrder.get(Relation.fromEdge(f0))) {
                    continue;
                }
                fList.add(f1);
            }
            fList.sort(Comparator.comparingInt(e -> rOrder.get(Relation.fromEdge(e))));
            for (Edge<VT, ET> f : fList) {
                stream.add(new State<>(phi1, f));
            }
        }

        if (stream.isEmpty()) {
            System.out.println("[OGFC_stream]: No size-1 pattern. Try to lower the support/confidence thresholds.");
            return new ArrayList<>();
        }

        Map<Integer, Set<OGFCRule<VT, ET>>> sieveSets = new HashMap<>();
        Map<Integer, Double> sieveVals = new HashMap<>();
        Map<Integer, Double> sieveCovs = new HashMap<>();

        int startIdx = (int) ((Math.log(maxPCov) / Math.log(1 + DEFAULT_EPSILON)) + 1);
        int endIdx = (int) (Math.log(maxPCov * topK) / Math.log(1 + DEFAULT_EPSILON));

        for (int i = startIdx; i <= endIdx; i++) {
            sieveSets.put(i, new HashSet<>());
            sieveVals.put(i, Math.pow(1 + DEFAULT_EPSILON, i));
            sieveCovs.put(i, 0.0);
        }

        int nSieved = 0;
        int nVerified = 0;

        while (!stream.isEmpty()) {
            if (nSieved >= topK * sieveSets.size()) {
                break;
            }
            if (nVerified > DEFAULT_MAXIMUM_STREAM_LENGTH) {
                break;
            }

            State<VT, ET> state = stream.removeFirst();
            nVerified++;

            OGFCRule<VT, ET> phi = OGFCRule.extendEdge(state.phi, state.f);

            phi.supp = getSupp(phi, positiveExamples);
            if (phi.supp < minSupp) {
                continue;
            }

            phi.conf = getConf(phi, positiveExamples.size());
            if (phi.conf < minConf) {
                continue;
            }

            double fp = phi.supp;
            double fn = getSupp(phi, negativeExamples);
            phi.gTest = getGTest(fp, fn, positiveExamples.size());
            phi.pCov = getPCov(phi.gTest, phi.supp);
            boolean isSieved = false;
            for (int i : sieveSets.keySet()) {
                Set<OGFCRule<VT, ET>> sieveSet_i = sieveSets.get(i);
                if (sieveSet_i.size() >= topK) {
                    continue;
                }

                double sVal_i = sieveVals.get(i);
                double sCov_i = sieveCovs.get(i);

                double sBound = (sVal_i / 2 - sCov_i) / (topK - sieveSet_i.size());

                double mg = getMarginalGain(phi, sieveSet_i, positiveExamples);
                if (mg < sBound) {
                    continue;
                }
                isSieved = true;
                sieveSet_i.add(phi);
                sieveCovs.put(i, sCov_i + mg);
                nSieved++;
            }

            // If the pattern is not in any sieve, continue to verify the next pattern.
            if (!isSieved) {
                continue;
            }

            // Identify if the pattern reaches the lattice boundary.
            // If not, find the frontiers for that pattern to extend.
            if (phi.P().numOfEdges() >= maxSize) {
                continue;
            }
            List<Edge<VT, ET>> fList = new ArrayList<>();
            for (Edge<VT, ET> f : phi.searchExtensionEdges()) {
                if (isPatternRepeated(f, state)) {
                    continue;
                }
                fList.add(f);
            }
            fList.sort(Comparator.comparingInt(e -> rOrder.get(Relation.fromEdge(e))));
            for (Edge<VT, ET> f : fList) {
                stream.add(new State<>(phi, f));
            }
        }

        int bestSieve = startIdx;
        double maxSCov = 0;
        for (int i : sieveCovs.keySet()) {
            if (sieveCovs.get(i) > maxSCov) {
                maxSCov = sieveCovs.get(i);
                bestSieve = i;
            }
        }

        if (sieveSets.get(bestSieve).isEmpty()) {
            System.out.println("[OGFC_stream]: No rules were found. Try to lower the support/confidence thresholds.");
        }

        return new ArrayList<>(sieveSets.get(bestSieve));
    }

    private double getMarginalGain(OGFCRule<VT, ET> phi, Set<OGFCRule<VT, ET>> sPSet, List<Edge<VT, ET>> positiveExamples) {
//        double sig = 2.0 / (1 + Math.exp(-phi.gTest)) - 1;
//        double minSect = positiveExamples.size();
//        for (OGFCRule<VT, ET> p : sPSet) {
//            double sect = 0;
//            for (Edge<VT, ET> e : positiveExamples) {
//                if (phi.matchSet().get(phi.x()).contains(e.srcNode())
//                        && phi.matchSet().get(phi.y()).contains(e.dstNode())
//                        && p.matchSet().get(p.x()).contains(e.srcNode())
//                        && p.matchSet().get(p.y()).contains(e.dstNode())) {
//                    sect++;
//                }
//            }
//            if (sect < minSect) {
//                minSect = sect;
//            }
//        }
//        double div = 1 - minSect / positiveExamples.size();

        double sig = 0;
        for (OGFCRule<VT, ET> phi2 : sPSet) {
            sig = sig + phi2.gTest;
        }
        sig = Math.sqrt(sig + phi.gTest) - Math.sqrt(sig);

        double div = 0;
        for (Edge<VT, ET> t : positiveExamples) {
            double Tt = 0;
            for (OGFCRule<VT, ET> phi2 : sPSet) {
                if (phi2.matchSet().get(phi2.x()).contains(t.srcNode())
                        && phi2.matchSet().get(phi2.y()).contains(t.dstNode())) {
                    Tt = Tt + phi2.supp;
                }
            }
            div = div + Math.sqrt(Tt);
        }
        div = div / positiveExamples.size();
        return sig + div;
    }

    private boolean isPatternRepeated(Edge<VT, ET> f, State<VT, ET> st) {
        if (st.f.srcId() != null && ((int) st.f.srcId() == (st.phi.P().numOfNodes() - 1))) {
            return false;
        }
        if (st.f.dstId() != null && ((int) st.f.dstId() == (st.phi.P().numOfNodes() - 1))) {
            return false;
        }
        double fOrder = rOrder.get(Relation.fromEdge(f));
        double fsOrder = rOrder.get(Relation.fromEdge(st.f));
        return fOrder > fsOrder;
    }
}
