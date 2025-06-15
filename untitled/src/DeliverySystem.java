public class DeliverySystem {
    // Parcel class to store delivery request details
    static class Parcel {
        String id;
        int location; // Delivery zone (node in graph)
        int deadline; // Deadline in minutes (e.g., 300 for 5 PM)
        int orderTime; // Time order was placed in minutes
        int priority; // Higher number = more urgent

        Parcel(String id, int location, int deadline, int orderTime, int priority) {
            this.id = id;
            this.location = location;
            this.deadline = deadline;
            this.orderTime = orderTime;
            this.priority = priority;
        }
    }

    // Vehicle class to store vehicle details
    static class Vehicle {
        String id;
        int currentLocation; // Current zone
        int capacity; // Number of parcels it can carry
        boolean isAvailable; // True if available, false if busy
        int availableTime; // Time when vehicle becomes available again

        Vehicle(String id, int currentLocation, int capacity) {
            this.id = id;
            this.currentLocation = currentLocation;
            this.capacity = capacity;
            this.isAvailable = true;
            this.availableTime = 0; // Available immediately
        }
    }

    // Failed delivery log entry
    static class FailedDelivery {
        Parcel parcel;
        String reason;

        FailedDelivery(Parcel parcel, String reason) {
            this.parcel = parcel;
            this.reason = reason;
        }
    }

    // Custom Max Heap for prioritizing parcels
    static class MaxHeap {
        Parcel[] heap;
        int size;
        int maxSize;

        MaxHeap(int maxSize) {
            this.maxSize = maxSize;
            this.size = 0;
            this.heap = new Parcel[maxSize];
        }

        private int parent(int index) { return (index - 1) / 2; }
        private int leftChild(int index) { return 2 * index + 1; }
        private int rightChild(int index) { return 2 * index + 2; }

        private boolean isHigherPriority(Parcel p1, Parcel p2) {
            if (p1.priority != p2.priority) return p1.priority > p2.priority;
            return p1.orderTime < p2.orderTime;
        }

        private void swap(int i, int j) {
            Parcel temp = heap[i];
            heap[i] = heap[j];
            heap[j] = temp;
        }

        void insert(Parcel parcel) {
            if (size >= maxSize) return;
            heap[size] = parcel;
            int current = size;
            size++;
            while (current > 0 && isHigherPriority(heap[current], heap[parent(current)])) {
                swap(current, parent(current));
                current = parent(current);
            }
        }

        Parcel extractMax() {
            if (size == 0) return null;
            Parcel max = heap[0];
            heap[0] = heap[size - 1];
            size--;
            if (size > 0) heapify(0);
            return max;
        }

        private void heapify(int index) {
            int largest = index;
            int left = leftChild(index);
            int right = rightChild(index);

            if (left < size && isHigherPriority(heap[left], heap[largest])) {
                largest = left;
            }
            if (right < size && isHigherPriority(heap[right], heap[largest])) {
                largest = right;
            }
            if (largest != index) {
                swap(index, largest);
                heapify(largest);
            }
        }
    }

    // Custom Hash Table for vehicle tracking
    static class VehicleHashTable {
        Vehicle[] table;
        int size;

        VehicleHashTable(int size) {
            this.size = size;
            this.table = new Vehicle[size];
        }

        private int hash(String id) {
            int hash = 0;
            for (char c : id.toCharArray()) {
                hash += c;
            }
            return hash % size;
        }

        void insert(Vehicle vehicle) {
            int index = hash(vehicle.id);
            while (table[index] != null) {
                index = (index + 1) % size; // Linear probing
            }
            table[index] = vehicle;
        }

        Vehicle find(String id) {
            int index = hash(id);
            int startIndex = index;
            while (table[index] != null) {
                if (table[index].id.equals(id)) return table[index];
                index = (index + 1) % size;
                if (index == startIndex) break;
            }
            return null;
        }

        Vehicle findNearestAvailable(int destination, int[][] graph, int currentTime, int deadline) {
            Vehicle nearest = null;
            int minDistance = Integer.MAX_VALUE;

            for (int i = 0; i < size; i++) {
                if (table[i] != null && table[i].isAvailable && table[i].capacity > 0 && table[i].availableTime <= currentTime) {
                    int distance = dijkstra(graph, table[i].currentLocation, destination);
                    if (distance != Integer.MAX_VALUE && currentTime + distance <= deadline) {
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearest = table[i];
                        }
                    }
                }
            }
            return nearest;
        }
    }

    // Simple Event Queue for time-based simulation
    static class EventQueue {
        static class Event {
            int time; // Time when event occurs
            Vehicle vehicle; // Vehicle to make available

            Event(int time, Vehicle vehicle) {
                this.time = time;
                this.vehicle = vehicle;
            }
        }

        Event[] queue;
        int size;
        int maxSize;
        int front;
        int rear;

        EventQueue(int maxSize) {
            this.maxSize = maxSize;
            this.queue = new Event[maxSize];
            this.size = 0;
            this.front = 0;
            this.rear = -1;
        }

        void enqueue(int time, Vehicle vehicle) {
            if (size >= maxSize) return;
            rear = (rear + 1) % maxSize;
            queue[rear] = new Event(time, vehicle);
            size++;
        }

        Event dequeue() {
            if (size == 0) return null;
            Event event = queue[front];
            front = (front + 1) % maxSize;
            size--;
            return event;
        }
    }

    // Graph for city map (adjacency list as matrix)
    static class Graph {
        int[][] adjList;
        int vertices;

        Graph(int vertices) {
            this.vertices = vertices;
            this.adjList = new int[vertices][vertices];
            for (int i = 0; i < vertices; i++) {
                for (int j = 0; j < vertices; j++) {
                    adjList[i][j] = 0;
                }
            }
        }

        void addEdge(int from, int to, int travelTime) {
            adjList[from][to] = travelTime;
            adjList[to][from] = travelTime;
        }
    }

    // Dijkstra's algorithm for shortest path
    static int dijkstra(int[][] graph, int start, int end) {
        int vertices = graph.length;
        int[] distances = new int[vertices];
        boolean[] visited = new boolean[vertices];

        for (int i = 0; i < vertices; i++) {
            distances[i] = Integer.MAX_VALUE;
            visited[i] = false;
        }
        distances[start] = 0;

        for (int i = 0; i < vertices - 1; i++) {
            int minDistance = Integer.MAX_VALUE;
            int minVertex = -1;

            for (int v = 0; v < vertices; v++) {
                if (!visited[v] && distances[v] < minDistance) {
                    minDistance = distances[v];
                    minVertex = v;
                }
            }

            if (minVertex == -1) break;
            visited[minVertex] = true;

            for (int v = 0; v < vertices; v++) {
                if (!visited[v] && graph[minVertex][v] != 0 && distances[minVertex] != Integer.MAX_VALUE) {
                    int newDist = distances[minVertex] + graph[minVertex][v];
                    if (newDist < distances[v]) {
                        distances[v] = newDist;
                    }
                }
            }
        }
        return distances[end];
    }

    // Methods to add parcels and vehicles dynamically
    static void addParcel(MaxHeap parcels, String id, int location, int deadline, int orderTime, int priority) {
        parcels.insert(new Parcel(id, location, deadline, orderTime, priority));
    }

    static void addVehicle(VehicleHashTable vehicles, String id, int location, int capacity) {
        vehicles.insert(new Vehicle(id, location, capacity));
    }

    // Main delivery system logic
    static void runDeliverySystem(MaxHeap parcels, VehicleHashTable vehicles, Graph city, int startTime) {
        EventQueue events = new EventQueue(10);
        FailedDelivery[] failedDeliveries = new FailedDelivery[10];
        int failedCount = 0;
        int currentTime = startTime;

        System.out.println("Delivery Plan (Starting at time " + startTime + " minutes):");

        while (parcels.size > 0 || events.size > 0) {
            // Process events to make vehicles available
            while (events.size > 0 && events.queue[events.front].time <= currentTime) {
                EventQueue.Event event = events.dequeue();
                event.vehicle.isAvailable = true;
                System.out.println("Time " + currentTime + ": Vehicle " + event.vehicle.id + " is now available.");
            }

            // Process next parcel
            if (parcels.size > 0) {
                Parcel parcel = parcels.extractMax();
                Vehicle vehicle = vehicles.findNearestAvailable(parcel.location, city.adjList, currentTime, parcel.deadline);

                if (vehicle != null) {
                    int travelTime = dijkstra(city.adjList, vehicle.currentLocation, parcel.location);
                    int eta = currentTime + travelTime;

                    if (eta <= parcel.deadline) {
                        vehicle.isAvailable = false;
                        vehicle.capacity--;
                        vehicle.currentLocation = parcel.location;
                        events.enqueue(eta, vehicle); // Schedule vehicle to be available after delivery

                        System.out.println("Time " + currentTime + ": Parcel " + parcel.id +
                                " assigned to Vehicle " + vehicle.id +
                                " for delivery to Zone " + parcel.location +
                                ", ETA: " + eta + " (Deadline: " + parcel.deadline + ")");
                        currentTime = eta; // Advance time to ETA
                    } else {
                        if (failedCount < failedDeliveries.length) {
                            failedDeliveries[failedCount++] = new FailedDelivery(parcel, "ETA (" + eta + ") exceeds deadline (" + parcel.deadline + ")");
                        }
                    }
                } else {
                    if (failedCount < failedDeliveries.length) {
                        failedDeliveries[failedCount++] = new FailedDelivery(parcel, "No suitable vehicle available");
                    }
                }
            } else {
                // No parcels, advance time to next event
                if (events.size > 0) {
                    currentTime = events.queue[events.front].time;
                }
            }
        }

        // Print failed deliveries
        if (failedCount > 0) {
            System.out.println("\nFailed Deliveries:");
            for (int i = 0; i < failedCount; i++) {
                System.out.println("Parcel " + failedDeliveries[i].parcel.id + ": " + failedDeliveries[i].reason);
            }
        }
    }

    // Main method with user input
    public static void main(String[] args) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.println("Do you want to use sample data (y/n)?");
        String choice = scanner.nextLine();

        // Create city graph (4 zones)
        Graph city = new Graph(4);
        city.addEdge(0, 1, 10);
        city.addEdge(0, 2, 15);
        city.addEdge(1, 2, 5);
        city.addEdge(1, 3, 20);
        city.addEdge(2, 3, 10);

        // Initialize parcels and vehicles
        MaxHeap parcels = new MaxHeap(10);
        VehicleHashTable vehicles = new VehicleHashTable(10);
        int startTime = 540; // 9 AM

        if (choice.equalsIgnoreCase("y")) {
            // Sample data
            addVehicle(vehicles, "V1", 0, 2);
            addVehicle(vehicles, "V2", 2, 1);
            addParcel(parcels, "P1", 3, 600, 100, 2); // Urgent, due by 10 AM
            addParcel(parcels, "P2", 1, 720, 120, 1); // Less urgent, due by 12 PM
            addParcel(parcels, "P3", 3, 550, 110, 1); // Due by 9:10 AM (will fail)
        } else {
            // User input
            System.out.println("Enter number of vehicles:");
            int numVehicles = scanner.nextInt();
            for (int i = 0; i < numVehicles; i++) {
                System.out.println("Enter vehicle ID, location (0-3), capacity:");
                String id = scanner.next();
                int location = scanner.nextInt();
                int capacity = scanner.nextInt();
                addVehicle(vehicles, id, location, capacity);
            }

            System.out.println("Enter number of parcels:");
            int numParcels = scanner.nextInt();
            for (int i = 0; i < numParcels; i++) {
                System.out.println("Enter parcel ID, location (0-3), deadline (minutes), order time, priority (1-5):");
                String id = scanner.next();
                int location = scanner.nextInt();
                int deadline = scanner.nextInt();
                int orderTime = scanner.nextInt();
                int priority = scanner.nextInt();
                addParcel(parcels, id, location, deadline, orderTime, priority);
            }
            scanner.nextLine(); // Clear buffer
        }

        runDeliverySystem(parcels, vehicles, city, startTime);
        scanner.close();
    }
}