package utils

object DatabaseSchema {
  val createTablesSQL = List(
    """
    CREATE TABLE IF NOT EXISTS users (
      id SERIAL PRIMARY KEY,
      email VARCHAR(255) UNIQUE NOT NULL,
      password VARCHAR(255) NOT NULL,
      first_name VARCHAR(100) NOT NULL,
      last_name VARCHAR(100) NOT NULL,
      phone VARCHAR(20) NOT NULL,
      average_rating DECIMAL(2,1),
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      is_active BOOLEAN DEFAULT TRUE
    )
    """,
    
    """
    CREATE TABLE IF NOT EXISTS vehicles (
      id SERIAL PRIMARY KEY,
      user_id BIGINT REFERENCES users(id),
      brand VARCHAR(50) NOT NULL,
      model VARCHAR(50) NOT NULL,
      year INTEGER NOT NULL,
      license_plate VARCHAR(20) UNIQUE NOT NULL,
      seats INTEGER NOT NULL,
      color VARCHAR(30) NOT NULL
    )
    """,
    
    """
    CREATE TABLE IF NOT EXISTS trips (
      id SERIAL PRIMARY KEY,
      driver_id BIGINT REFERENCES users(id),
      vehicle_id BIGINT REFERENCES vehicles(id),
      departure_city VARCHAR(100) NOT NULL,
      arrival_city VARCHAR(100) NOT NULL,
      departure_time TIMESTAMP NOT NULL,
      arrival_time TIMESTAMP NOT NULL,
      available_seats INTEGER NOT NULL,
      price_per_seat DECIMAL(10,2) NOT NULL,
      description TEXT,
      is_active BOOLEAN DEFAULT TRUE,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """,
    
    """
    CREATE TABLE IF NOT EXISTS reservations (
      id SERIAL PRIMARY KEY,
      trip_id BIGINT REFERENCES trips(id),
      passenger_id BIGINT REFERENCES users(id),
      seats_reserved INTEGER NOT NULL,
      total_price DECIMAL(10,2) NOT NULL,
      status VARCHAR(20) DEFAULT 'CONFIRMED',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """,
    
    """
    CREATE TABLE IF NOT EXISTS payments (
      id SERIAL PRIMARY KEY,
      reservation_id BIGINT REFERENCES reservations(id),
      payer_id BIGINT REFERENCES users(id),
      receiver_id BIGINT REFERENCES users(id),
      amount DECIMAL(10,2) NOT NULL,
      status VARCHAR(20) DEFAULT 'COMPLETED',
      payment_method VARCHAR(50) DEFAULT 'SIMULATION',
      transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """,
    
    """
    CREATE TABLE IF NOT EXISTS ratings (
      id SERIAL PRIMARY KEY,
      trip_id BIGINT REFERENCES trips(id),
      rater_id BIGINT REFERENCES users(id),
      rated_id BIGINT REFERENCES users(id),
      rating INTEGER CHECK (rating >= 1 AND rating <= 5),
      comment TEXT,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """,
    
    """
    CREATE TABLE IF NOT EXISTS messages (
      id SERIAL PRIMARY KEY,
      sender_id BIGINT REFERENCES users(id),
      receiver_id BIGINT REFERENCES users(id),
      subject VARCHAR(255) NOT NULL,
      content TEXT NOT NULL,
      is_read BOOLEAN DEFAULT FALSE,
      sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """
  )
}