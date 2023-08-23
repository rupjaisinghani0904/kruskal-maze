import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

/*
 * Press "R" to restart Maze
 * Press "B" for BFS
 * Press "D" for DFS
 * Press "C" to clear maze
 * Press "T" to toggle searched cells
 * ArrayList<edges> field in maze class represent edges in the 
 * maze and not the edges in the tree
 * ArrayList<edges> field in Cell class represent edges in the 
 * tree that this cell can travel to
 * Maze starts off as grid
 * Kruskal algorithm removed edge randomly one by one 
 * until union is created between first cell and last cell
 */

// represents an edge
class Edge {
  Cell from;
  Cell to;
  int weight;

  Edge(Cell from, Cell to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  // override equal
  public boolean equals(Object o) {
    if (!(o instanceof Edge)) {
      return false;
    }
    Edge other = (Edge) o;
    return (this.from.equals(other.from) 
        && this.to.equals(other.to) 
        && this.weight == other.weight)
        || (this.from.equals(other.to) 
            && this.to.equals(other.from)
            && this.weight == other.weight);
  }

  // overrides hashcode method
  public int hashCode() {
    return (this.from.x + this.from.y + this.to.x + this.to.y)
        * this.weight * 10000;
  }

  // draw horizontal edge
  public WorldImage drawHorzEdge() {
    return new RectangleImage(10, 1, OutlineMode.SOLID, Color.BLACK);
  }

  // draw vertical edge
  public WorldImage drawVertEdge() {
    return new RectangleImage(1, 10, OutlineMode.SOLID, Color.BLACK);
  }

}

// compares the weights of two edges
class EdgeComparator implements Comparator<Edge> {
  EdgeComparator() {
  }

  // compares the edges by weight
  public int compare(Edge e1, Edge e2) {
    return e1.weight - e2.weight;
  }

}

//to represent a Cell
class Cell  {
  int x;
  int y;
  ArrayList<Edge> outer;
  boolean searched;
  boolean correct;
  Color color;

  Cell(int x, int y, ArrayList<Edge> outer) {
    this.x = x;
    this.y = y;
    this.outer = outer;
    this.searched = false;
    this.correct = false;
    this.color = Color.GRAY;
  }

  // overrides equals
  public boolean equals(Object object) {
    if (!(object instanceof Cell )) {
      return false;
    }
    Cell  other = (Cell) object;
    return (this.x == other.x && this.y == other.y
        && this.outer.equals(other.outer));
  }

  // overrides hashcode 
  public int hashCode() {
    return (((this.x * 5) - this.y) * 10000) - this.outer.hashCode();
  }

  // draw a Cell 
  WorldImage drawCell() {
    return new RectangleImage(10, 10, OutlineMode.SOLID, this.color);
  }

  // determines if this cell is above given cell
  public boolean above(Cell to) {
    return this.y + 10 == to.y;
  }

  // changes color of this cell
  public void changeColor(Color c) {
    this.color = c;
  }

}

// represents the maze
class Maze extends World {
  int width;
  int height;
  ArrayList<ArrayList<Cell>> board;
  ArrayList<Cell> searchedCells;
  ArrayList<Cell> pathCells;
  HashMap<Cell, Cell> representatives;
  ArrayList<Edge> edgesInTree;
  ArrayList<Edge> edges;
  Random rand;
  boolean toggleSeen;

  // the constructor for testing
  Maze(int width, int height, Random rand) {
    if (width < 2 || width > 100) {
      throw new IllegalArgumentException("Width of maze has to be between 2 and 100");
    }
    else {
      this.width = width;
    }
    if (height < 2 || height > 60) {
      throw new IllegalArgumentException("Height of maze has to be between 2 and 60");
    }
    else {
      this.height = height;
    }

    this.rand = rand;
    this.board = this.makeBoard();
    this.edgesInTree = new ArrayList<Edge>();
    this.edges = this.allEdges();
    this.edges.sort(new EdgeComparator());
    this.kruskal();
    this.searchedCells = new ArrayList<Cell>();
    this.pathCells = new ArrayList<Cell>();
    this.connectEdges();
    this.toggleSeen = true;
  }

  // constructor for playing
  Maze(int width, int height) {
    if (width < 2 || width > 100) {
      throw new IllegalArgumentException("Width of maze has to be between 2 and 100");
    }
    else {
      this.width = width;
    }
    if (height < 2 || height > 60) {
      throw new IllegalArgumentException("Height of maze has to be between 2 and 60");
    }
    else {
      this.height = height;
    }
    this.rand = new Random();
    this.board = this.makeBoard();
    this.edgesInTree = new ArrayList<Edge>();
    this.edges = this.allEdges();
    this.edges.sort(new EdgeComparator());
    this.kruskal();
    this.searchedCells = new ArrayList<Cell>();
    this.pathCells = new ArrayList<Cell>();
    this.connectEdges();
    this.toggleSeen = true;
  }

  // generates a gris of cells
  ArrayList<ArrayList<Cell>> makeBoard() {
    int x = 5;
    int y = -5;
    ArrayList<ArrayList<Cell>> finalBoard = new ArrayList<ArrayList<Cell>>();
    for (int i = 0; i < this.height; i++) {
      ArrayList<Cell> temp = new ArrayList<Cell>();
      finalBoard.add(temp);
      x = 5;
      y = y + 10;
      for (int j = 0; j < this.width; j++) {
        ArrayList<Edge> edge = new ArrayList<Edge>();
        Cell cell = new Cell(x, y, edge);
        x = x + 10;
        finalBoard.get(i).add(cell);
      }
    }
    finalBoard.get(0).get(0).changeColor(Color.GREEN);
    finalBoard.get(this.height - 1).get(this.width - 1).changeColor(Color.PINK);
    return finalBoard;
  }

  // replaces the Cell with the supplied Cell
  void union(HashMap<Cell, Cell> represent, Cell cell1, Cell cell2) {
    represent.put(cell1, cell2);
  }

  // finds the parent Cell at the given point
  Cell find(HashMap<Cell, Cell> represent, Cell cell) {
    if (represent.get(cell).equals(cell)) {
      return represent.get(cell);
    }
    else {
      return find(represent, represent.get(cell));
    }
  }

  // removes random edge until first cell and last cell are in the same union
  List<Edge> kruskal() {
    HashMap<Cell, Cell> rep = new HashMap<Cell, Cell>();

    for (ArrayList<Cell> list : this.board) {
      for (Cell cell : list) {
        rep.put(cell, cell);
      }
    }

    for (Edge e : this.edges) {
      if (!find(rep, e.from).equals(find(rep, e.to))) {
        this.edgesInTree.add(e);
        union(rep, find(rep, e.from), (find(rep, e.to)));
      }
    }
    this.edges.removeAll(this.edgesInTree);
    this.representatives = rep;
    return this.edgesInTree;
  }

  // connects the edges in the edgesInTree to the board
  void connectEdges() {
    for (Edge edge : this.edgesInTree) {
      edge.from.outer.add(edge);
      edge.to.outer.add(edge);
    }
  }

  // creates a list of all edges that are possible to generate
  ArrayList<Edge> allEdges() {

    ArrayList<Edge> answer = new ArrayList<Edge>();

    for (int i = 0; i < this.board.size(); i++) {
      for (int j = 0; j < this.board.get(i).size(); j++) {

        if (i < height - 1) {
          Cell from = board.get(i).get(j);
          Cell to = board.get(i + 1).get(j);
          Edge edge = new Edge(from, to, this.rand.nextInt(1000));
          answer.add(edge);
        }

        if (j < width - 1) {
          Cell from = board.get(i).get(j);
          Cell to = board.get(i).get(j + 1);
          Edge edge = new Edge(from, to, this.rand.nextInt(1000));
          answer.add(edge);
        }
      }
    }
    return answer;
  }

  // makes the scene
  public WorldScene makeScene() {

    WorldScene finalScene = new WorldScene(1300, 800);

    WorldImage instruction1 = new TextImage("Press 'D' for depth-first search", 
        10, FontStyle.REGULAR,
        Color.BLACK);

    finalScene.placeImageXY(instruction1, 1150, 100);

    WorldImage instruction2 = new TextImage("Press 'B' for breadth-first search", 
        10, FontStyle.REGULAR,
        Color.BLACK);

    finalScene.placeImageXY(instruction2, 1150, 200);

    WorldImage instruction3 = new TextImage("Press 'R' to restart the maze", 
        10, FontStyle.REGULAR, 
        Color.BLACK);

    WorldImage instruction4 = new TextImage("Press 'C' to clear the maze", 
        10, FontStyle.REGULAR, 
        Color.BLACK);

    WorldImage instruction5 = new TextImage("Press 'T' to toggle the searched cells", 
        10, FontStyle.REGULAR, 
        Color.BLACK);
    finalScene.placeImageXY(instruction3, 1150, 300);
    finalScene.placeImageXY(instruction4, 1150, 400);
    finalScene.placeImageXY(instruction5, 1150, 500);

    // draw the grid
    for (ArrayList<Cell> list : this.board) {
      for (Cell c : list) {
        finalScene.placeImageXY(c.drawCell(), c.x,
            c.y);
      }
    }

    // draws the edges
    for (Edge edge : this.edges) {
      if (edge.from.above(edge.to)) {
        finalScene.placeImageXY(edge.drawHorzEdge(), edge.from.x,
            edge.from.y + 5);
      }
      else {
        finalScene.placeImageXY(edge.drawVertEdge(), edge.from.x + 5,
            edge.from.y);
      }
    }

    return finalScene;
  }

  // moves the player and also switches from breath-first and depth-first search.
  public void onKeyEvent(String ke) {

    if (ke.equals("b") && this.board.get(0).get(0).color.equals(Color.GREEN)) {
      this.searchedCells.clear();
      this.pathCells.clear();
      this.solveMaze(ke);
    }
    if (ke.equals("d") && this.board.get(0).get(0).color.equals(Color.GREEN)) {
      this.searchedCells.clear();
      this.pathCells.clear();
      this.solveMaze(ke);
    }
    if (ke.equals("c")) {
      this.searchedCells.clear();
      this.pathCells.clear();
      for (ArrayList<Cell> row : this.board) {
        for (Cell c : row) {
          c.changeColor(Color.GRAY);
          c.correct = false;
          c.searched = false;
        }
      }
      this.board.get(0).get(0).changeColor(Color.GREEN);
      this.board.get(this.height - 1).get(this.width - 1).changeColor(Color.PINK);
      this.toggleSeen = true;
    }
    if (ke.equals("t") && this.board.get(0).get(0).color.equals(Color.PINK)) {
      if (this.toggleSeen) {
        this.toggleSeen = false;
      }
      else {
        this.toggleSeen = true;
      }
    }
    if (ke.equals("r")) {
      Maze restart = new Maze(this.width, this.height, this.rand);
      this.board = restart.board;
      this.representatives = restart.representatives;
      this.edgesInTree = restart.edgesInTree;
      this.edges = restart.edges;
      this.searchedCells = restart.searchedCells;
      this.pathCells = restart.pathCells;
      this.toggleSeen = true;
    }
  }

  // solves the maze BFS or DFS given the string provided
  void solveMaze(String ke) {
    HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();
    ArrayList<Cell> worklist = new ArrayList<Cell>();
    ArrayList<Cell> seen = new ArrayList<Cell>();
    worklist.add(this.board.get(0).get(0));

    while (worklist.size() > 0) {
      Cell next = worklist.remove(0);
      Cell last = this.board.get(this.board.size() - 1).get(this.board.get(0).size() - 1);
      if (next.equals(last)) {
        this.reconstruct(cameFromEdge, next);
        return;
      }
      for (Edge edge : next.outer) {
        if (!seen.contains(edge.to) && next.equals(edge.from)) {
          if (ke.equals("d")) {
            worklist.add(0, edge.to);
          }

          if (ke.equals("b")) {
            worklist.add(edge.to);
          }

          this.searchedCells.add(next);
          seen.add(next);
          cameFromEdge.put(edge.to, next);
        }
        else if (!seen.contains(edge.from) && next.equals(edge.to)) {
          if (ke.equals("d")) {
            worklist.add(0, edge.from);
          }

          if (ke.equals("b")) {
            worklist.add(edge.from);
          }

          this.searchedCells.add(next);
          seen.add(next);
          cameFromEdge.put(edge.from, next);
        }
      }
    }
  }

  // reconstructs the path from the end to the beginning
  public void reconstruct(HashMap<Cell, Cell> cameFromEdge, Cell next) {
    this.pathCells.add(this.board.get(this.board.size() - 1).get(this.board.get(0).size() - 1));
    Cell initial = this.board.get(0).get(0);
    initial.correct = true;
    while (initial != next) {
      next.correct = true;
      this.pathCells.add(cameFromEdge.get(next));
      next = cameFromEdge.get(next);
    }
  }

  // onTick method
  public void onTick() {

    if (this.searchedCells.size() > 0) {
      Cell seen = this.searchedCells.remove(0);
      seen.color = Color.BLUE;
      seen.searched = true;
    }

    else if (this.pathCells.size() > 0) {
      Cell path = this.pathCells.remove(0);
      path.color = Color.PINK;
    }
    if (!this.toggleSeen) {
      for (ArrayList<Cell> row : this.board) {
        for (Cell c : row) {
          if (c.searched && !c.correct) {
            c.changeColor(Color.GRAY);
          }
        }
      }
    }
    else {
      for (ArrayList<Cell> row : this.board) {
        for (Cell c : row) {
          if (c.searched && !c.correct) {
            c.changeColor(Color.BLUE);
          }
        }
      }
    }
  }

}

//tests and examples
class ExamplesMaze {

  void testMazeGame(Tester t) {
    Maze game = new Maze(100, 60, new Random(2));
    game.bigBang(1300, 800, 0.001);
  }

  Maze maze;
  Cell a;
  Cell b;
  Cell c;
  Cell d;
  Cell e;
  Cell f;

  Edge aToB;
  Edge aToD;
  Edge bToC;
  Edge bToE;
  Edge dToE;
  Edge cToF;
  Edge eToF;

  ArrayList<Edge> listEdge;

  ArrayList<ArrayList<Cell>> board1;

  ArrayList<Cell> list1;

  EdgeComparator comp;

  HashMap<Cell, Cell> hashMap;

  // initData
  void initData() {
    maze = new Maze(3, 2, new Random(2));

    a = new Cell(5, 5, new ArrayList<Edge>());
    b = new Cell(15, 5, new ArrayList<Edge>());
    c = new Cell(25, 5, new ArrayList<Edge>());
    d = new Cell(5, 15, new ArrayList<Edge>());
    e = new Cell(15, 15, new ArrayList<Edge>());
    f = new Cell(25, 15, new ArrayList<Edge>());

    aToB = new Edge(this.a, this.b, 30);
    aToD = new Edge(this.a, this.d, 50);
    bToC = new Edge(this.b, this.c, 40);
    bToE = new Edge(this.b, this.e, 35);
    dToE = new Edge(this.d, this.e, 15);
    cToF = new Edge(this.c, this.f, 25);
    eToF = new Edge(this.e, this.f, 50);

    comp = new EdgeComparator();

    hashMap = new HashMap<Cell, Cell>();

    listEdge = new ArrayList<Edge>(Arrays.asList(this.aToB, this.aToD, this.bToC, this.bToE,
        this.bToE, this.dToE, this.cToF, this.eToF));
  }

  // tests onKeyEvent method
  void testOnKeyEvent(Tester t) {
    initData();
    this.maze.onKeyEvent("b");
    t.checkExpect(this.maze.searchedCells.size() > 0, true);
    t.checkExpect(this.maze.pathCells.size() > 0, true);
    t.checkExpect(this.maze.toggleSeen, true);
    this.maze.onKeyEvent("r");
    t.checkExpect(this.maze.searchedCells.size() == 0, true);
    t.checkExpect(this.maze.pathCells.size() == 0, true);
    t.checkExpect(this.maze.toggleSeen, true);
    this.maze.onKeyEvent("d");
    t.checkExpect(this.maze.searchedCells.size() > 0, true);
    t.checkExpect(this.maze.pathCells.size() > 0, true);
    t.checkExpect(this.maze.toggleSeen, true);
    this.maze.onKeyEvent("c");
    t.checkExpect(this.maze.searchedCells.size() == 0, true);
    t.checkExpect(this.maze.pathCells.size() == 0, true);
    t.checkExpect(this.maze.toggleSeen, true);
    this.maze.board.get(0).get(0).color = Color.PINK;
    this.maze.onKeyEvent("t");
    t.checkExpect(this.maze.toggleSeen, false);
    this.maze.onKeyEvent("t");
    t.checkExpect(this.maze.toggleSeen, true);
  }

  // tests the solveMaze method
  void testSolveMaze(Tester t) {
    initData();
    this.maze.solveMaze("b");
    t.checkExpect(this.maze.searchedCells.size() > 0, true);
    t.checkExpect(this.maze.pathCells.get(0), this.maze.board
        .get(this.maze.board.size() - 1).get(this.maze.board.get(0).size() - 1));
    t.checkExpect(this.maze.pathCells.get(this.maze.pathCells.size() - 1),
        this.maze.board.get(0).get(0));

    this.maze.solveMaze("d");
    t.checkExpect(this.maze.searchedCells.size() > 0, true);
    t.checkExpect(this.maze.pathCells.get(0), this.maze.board
        .get(this.maze.board.size() - 1).get(this.maze.board.get(0).size() - 1));
    t.checkExpect(this.maze.pathCells.get(this.maze.pathCells.size() - 1),
        this.maze.board.get(0).get(0));
  }

  // tests the reconstruct method
  void testReconstruct(Tester t) {
    initData();

    ArrayList<Cell> row1 = new ArrayList<Cell>(Arrays.asList(a, b, c));
    ArrayList<Cell> row2 = new ArrayList<Cell>(Arrays.asList(d, e, f));

    ArrayList<ArrayList<Cell>> board1 = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(row1, row2));

    HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();
    cameFromEdge.put(b, a);
    cameFromEdge.put(c, b);
    cameFromEdge.put(d, c);
    cameFromEdge.put(e, d);
    cameFromEdge.put(f, d);

    ArrayList<Cell> reconstruct1 = new ArrayList<Cell>(Arrays.asList(f, c, b, a));

    this.maze.board = board1;

    this.maze.reconstruct(cameFromEdge, d);
    t.checkExpect(this.maze.pathCells, reconstruct1);
  }

  // tests ontick method
  void testOnTick(Tester t) {
    initData();
    t.checkExpect(this.maze.searchedCells.size(), 0);
    t.checkExpect(this.maze.pathCells.size(), 0);

    this.maze.onKeyEvent("b");
    this.maze.onTick();
    t.checkExpect(this.maze.searchedCells.size(), 4);
    t.checkExpect(this.maze.pathCells.size(), 6);

    this.maze.onTick();
    t.checkExpect(this.maze.searchedCells.size(), 3);
    t.checkExpect(this.maze.pathCells.size(), 6);

  }

  // testing the equals method in the edge class
  void testEdgeEquals(Tester t) {
    initData();
    t.checkExpect(this.aToB.equals(this.aToB), true);
    t.checkExpect(this.bToE.equals(this.aToB), false);
    t.checkExpect(this.aToB.equals(new Edge(this.a, this.b, 30)), true);

  }

  // testing the hashCode in the edge class
  void testEdgeHashCode(Tester t) {
    initData();
    t.checkExpect(this.aToB.hashCode(), 9000000);
    t.checkExpect(this.aToB.hashCode(), (new Edge(this.a, this.b, 30).hashCode()));
    t.checkExpect(this.aToB.hashCode(), (new Edge(this.b, this.a, 30)).hashCode());

  }

  // testing the equals method in the Cell class
  void testCellEquals(Tester t) {
    initData();
    t.checkExpect(this.a.equals(this.a), true);
    t.checkExpect(this.a.equals(this.b), false);
    t.checkExpect(this.a.equals(new Cell(5, 5, new ArrayList<Edge>())), true);
    t.checkExpect(this.b.equals(new Cell(15, 5, new ArrayList<Edge>())), true);
    t.checkExpect(this.f.equals(new Cell(1, 2, new ArrayList<Edge>())), false);

  }

  // testing the hashCode in the Cell class
  void testCellHashCode(Tester t) {
    initData();

    t.checkExpect(this.a.hashCode(), 199999);
    t.checkExpect(this.b.hashCode(), 699999);
    t.checkExpect(
        this.e.hashCode() == (new Cell(15, 15, new ArrayList<Edge>()).hashCode()), true);
    t.checkExpect(
        this.f.hashCode() == (new Cell(25, 15, new ArrayList<Edge>()).hashCode()), true);
    t.checkExpect(this.c.hashCode(),
        (new Cell(25, 5, new ArrayList<Edge>()).hashCode()));
  }

  // tests the drawEdge method
  void testDrawEdge(Tester t) {
    initData();

    // drawing all example edges
    t.checkExpect(this.aToB.drawVertEdge(), 
        new RectangleImage(1, 10, OutlineMode.SOLID, Color.BLACK));
    t.checkExpect(this.aToB.drawHorzEdge(), 
        new RectangleImage(10, 1, OutlineMode.SOLID, Color.BLACK));
  }

  // tests the compare method
  void testCompare(Tester t) {
    initData();
    t.checkExpect(this.comp.compare(aToB, aToB), 0);
  }

  // tests the drawCell Cell method
  void testDrawCell(Tester t) {
    initData();

    // drawing the Cell  Cell for every Cell in examples
    t.checkExpect(this.a.drawCell(), 
        new RectangleImage(10, 10, OutlineMode.SOLID, Color.gray));
    t.checkExpect(this.b.drawCell(), 
        new RectangleImage(10, 10, OutlineMode.SOLID, Color.gray));
  }

  // testing the makeBoard method
  void testMakeBoard(Tester t) {
    initData();  
    a.color = Color.GREEN;
    f.color = Color.PINK;
    ArrayList<Cell> row1 = new ArrayList<Cell>();
    row1.add(this.a);
    row1.add(this.b);
    row1.add(this.c);
    ArrayList<Cell> row2 = new ArrayList<Cell>();
    row2.add(this.d);
    row2.add(this.e);
    row2.add(this.f);
    ArrayList<ArrayList<Cell>> board = new ArrayList<ArrayList<Cell>>();
    board.add(row1);
    board.add(row2);

    t.checkExpect(this.maze.makeBoard(), board);
  }

  // testing the union method
  void testUnion(Tester t) {
    initData();

    t.checkExpect(this.hashMap.isEmpty(), true);
    this.maze.union(this.hashMap, a, b);
    t.checkExpect(this.hashMap.isEmpty(), false);
    t.checkExpect(this.hashMap.size(), 1);
    t.checkExpect(this.hashMap.containsKey(a), true);
    t.checkExpect(this.hashMap.containsValue(b), true);
    t.checkExpect(this.hashMap.containsKey(b), false);
    t.checkExpect(this.hashMap.containsValue(a), false);
  }

  // testing the find method
  void testFind(Tester t) {
    initData();
    this.hashMap.put(this.a, this.e);
    this.hashMap.put(this.b, this.a);
    this.hashMap.put(this.c, this.e);
    this.hashMap.put(this.d, this.e);
    this.hashMap.put(this.e, this.e);
    this.hashMap.put(this.f, this.d);

    t.checkExpect(this.maze.find(hashMap, this.a), this.e);
    t.checkExpect(this.maze.find(hashMap, this.b), this.e);
  }

  // tests the minimumSpanningTree method
  void testKruskal(Tester t) {
    initData();
    this.maze.representatives.clear();
    for (ArrayList<Cell> row : this.maze.board) {
      for (Cell c : row) {
        this.maze.representatives.put(c, c);
      }
    }
    t.checkExpect(this.maze.find(this.maze.representatives, 
        this.maze.board.get(0).get(0)), this.maze.board.get(0).get(0));
    this.maze.kruskal();
    t.checkExpect(this.maze.find(this.maze.representatives, 
        this.maze.board.get(0).get(0)), this.maze.board.get(0).get(1));
  }

  // tests the connect edges method
  void testConnectEdges(Tester t) {
    initData();
    this.maze.connectEdges();
    for (Edge test : this.maze.edgesInTree) {
      t.checkExpect(test.from.outer.contains(test), true);
      t.checkExpect(test.to.outer.contains(test), true);
    }
  }

  // tests the allEdges method
  void testAllEdges(Tester t) {
    initData();
    Maze test = new Maze(40, 40, new Random(1));
    t.checkExpect(test.allEdges().size(), 3120);
    t.checkExpect(this.maze.allEdges().size(), 7);
  }

  // tests makeScene method
  void testMakeScene(Tester t) {
    initData();

    WorldScene scene = new WorldScene(1300, 800);
    WorldImage a = new RectangleImage(10, 10, OutlineMode.SOLID, Color.GREEN);
    WorldImage b = new RectangleImage(10, 10, OutlineMode.SOLID, Color.GRAY);
    WorldImage c = new RectangleImage(10, 10, OutlineMode.SOLID, Color.GRAY);
    WorldImage d = new RectangleImage(10, 10, OutlineMode.SOLID, Color.GRAY);
    WorldImage e = new RectangleImage(10, 10, OutlineMode.SOLID, Color.GRAY);
    WorldImage f = new RectangleImage(10, 10, OutlineMode.SOLID, Color.PINK);

    WorldImage e1 = new RectangleImage(1, 10, OutlineMode.SOLID, Color.BLACK);
    WorldImage e2 = new RectangleImage(1, 10, OutlineMode.SOLID, Color.BLACK);

    WorldImage text1 = new TextImage("Press 'D' for depth-first search", 
        10, FontStyle.REGULAR, Color.BLACK);
    WorldImage text2 = new TextImage("Press 'B' for breadth-first search", 
        10, FontStyle.REGULAR, Color.BLACK);
    WorldImage text3 = new TextImage("Press 'R' to restart the maze", 
        10, FontStyle.REGULAR, Color.BLACK);
    WorldImage text4 = new TextImage("Press 'C' to clear the maze", 
        10, FontStyle.REGULAR, Color.BLACK);
    WorldImage text5 = new TextImage("Press 'T' to toggle the searched cells", 
        10, FontStyle.REGULAR, Color.BLACK);
    scene.placeImageXY(text1, 1150, 100);
    scene.placeImageXY(text2, 1150, 200);
    scene.placeImageXY(text3, 1150, 300);
    scene.placeImageXY(text4, 1150, 400);
    scene.placeImageXY(text5, 1150, 500);
    scene.placeImageXY(a, 5, 5);
    scene.placeImageXY(b, 15, 5);
    scene.placeImageXY(c, 25, 5);
    scene.placeImageXY(d, 5, 15);
    scene.placeImageXY(e, 15, 15);
    scene.placeImageXY(f, 25, 15);
    scene.placeImageXY(e1, 10, 5);
    scene.placeImageXY(e2, 20, 15);

    t.checkExpect(this.maze.makeScene(), scene);
  }

  // test for above method
  void testAbove(Tester t) {
    initData();
    t.checkExpect(this.a.above(this.d), true);
    t.checkExpect(this.a.above(this.c), false);
  }

  //test for exceptions
  void testExceptions(Tester t) {
    initData();
    t.checkConstructorException(
        new IllegalArgumentException("Height of maze has to be between 2 and 60"),
        "Maze",
        10, 61, new Random());
    t.checkConstructorException(
        new IllegalArgumentException("Width of maze has to be between 2 and 100"),
        "Maze",
        101, 10, new Random());
  } 
}