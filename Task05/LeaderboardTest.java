package Task05;

import java.util.*;

/**
 * Task05: Rigorous Testing of Generic Ranking Component
 * 
 * 1. Multi-level ties test: Points, Goal Difference, and Name.
 * 2. Faulty comparator test: Integer subtraction overflow bug and tie-breaker omission.
 * 3. Reusability verification: Demonstrating Leaderboard<T> on another entity (QuizTeam).
 */
public class LeaderboardTest {

    static class QuizTeam {
        final String college;
        final int score;
        final int penalties;

        QuizTeam(String college, int score, int penalties) {
            this.college = college;
            this.score = score;
            this.penalties = penalties;
        }

        @Override
        public String toString() {
            return college + "(Score:" + score + ", Pen:" + penalties + ")";
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  Task 05: Leaderboard Principle Verification & Edge Case Testing");
        System.out.println("==========================================================================\n");

        // 1. Multi-level ties test
        System.out.println("--- Test 1: Multi-level Ties Test ---");
        Comparator<Team> robustComparator = Comparator
                .comparingInt((Team t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                .thenComparing((Team t) -> t.name);

        Leaderboard<Team> robustBoard = new Leaderboard<>(robustComparator);
        robustBoard.add(new Team("Tigers", 10, 5));
        robustBoard.add(new Team("Lions", 10, 5));    // Same pts, same GD -> alphabetical tiebreak (Lions < Tigers)
        robustBoard.add(new Team("Bears", 10, 7));    // Same pts, higher GD -> ranks first among 10-pointers
        robustBoard.add(new Team("Wolves", 8, 12));   // Lower pts, higher GD -> points take precedence
        
        System.out.println("Input: Tigers(10,5), Lions(10,5), Bears(10,7), Wolves(8,12)");
        System.out.println("Output: " + robustBoard.ranking());
        System.out.println("Explanation: Bears(10,7) first (GD 7 > 5). Lions(10,5) beats Tigers(10,5) on name. Wolves(8,12) last because points (10 > 8) takes absolute priority.\n");

        // 2. Faulty comparator: Subtraction causing integer overflow
        System.out.println("--- Test 2: Breaking Faulty Comparator (Subtraction Overflow) ---");
        // Naive comparator using subtraction: (b - a)
        Comparator<Team> faultySubtraction = (t1, t2) -> (t2.points - t1.points);
        Team normalTeam = new Team("Alpha", 100, 0);
        Team extremeTeam = new Team("Omega", Integer.MIN_VALUE, 0);

        int comparisonResult = faultySubtraction.compare(normalTeam, extremeTeam);
        System.out.println("Normal points: 100, Extreme points: " + Integer.MIN_VALUE);
        System.out.println("Faulty (t2.points - t1.points) evaluation: " + comparisonResult);
        System.out.println("Did overflow invert rank order? " + (comparisonResult > 0 ? "YES! ExtremeTeam falsely ranked higher due to integer overflow." : "No"));

        int correctResult = Integer.compare(normalTeam.points, extremeTeam.points);
        System.out.println("Correct Integer.compare(100, MIN_VALUE): " + correctResult + " (Alpha correctly ranked above Omega)\n");

        // 3. Reusability Verification on QuizTeam
        System.out.println("--- Test 3: Reusing Leaderboard<T> for QuizTeam ---");
        Comparator<QuizTeam> quizOrder = Comparator
                .comparingInt((QuizTeam q) -> q.score).reversed()
                .thenComparingInt(q -> q.penalties) // Lower penalties first
                .thenComparing(q -> q.college);

        Leaderboard<QuizTeam> quizBoard = new Leaderboard<>(quizOrder);
        quizBoard.add(new QuizTeam("IIT Bombay", 240, 2));
        quizBoard.add(new QuizTeam("BITS Pilani", 240, 1)); // Same score, fewer penalties
        quizBoard.add(new QuizTeam("NIT Trichy", 210, 0));

        System.out.println("Quiz Leaderboard Ranking:");
        System.out.println(quizBoard.ranking());
        System.out.println("Proves Leaderboard<T> is 100% decoupled and generic!");
    }
}
