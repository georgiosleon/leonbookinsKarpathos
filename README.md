



### App Params 

    sourceEncoding: UTF-8 
    java.version:   19     



### To Start   use Spring boot  maven plug-in    in idea   >>>   
    Step (1)    leonbookinsKarpathos [package]
    Step (2)    leonbookinsKarpathos [spring-boot:run]   


### DB CREATION

 
    C:\DEVC\leonbookinsKarpathos\dev\app\leonbookinsKarpathos\src\main\resources\persistence-sqlite.properties

### JPA 
    
    spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
        hibernate.hbm2ddl.auto=none
        --hibernate.hbm2ddl.auto=create

### React frontend

The React/Vite source is in `frontend/`. It shares one booking screen between
rooms and vehicles, uses the existing vis-timeline browser library, and calls
the current Spring booking endpoints. User management and authentication are
not part of this phase. Active/paid bookings and cancelled bookings appear in
separate timeline panels.

Open `/?type=rooms&page=reports` or `/?type=cars&page=reports` for the reports
page. It summarizes bookings from the existing database-backed
`GET /booking/getAll` endpoint, filters by arrival date and resource type, and
provides detailed HTML and PDF exports using the existing report endpoints.

The global frontend save behavior defaults to creating a new booking when
`VITE_BOOKING_SAVE_MODE` is unset. Set it to `edit` at build time to default to
updating a selected booking instead. A frontend checkbox can switch between
create and edit modes at runtime for testing; its choice lasts for the current
browser tab session. With edit mode enabled, submitting an unselected form
still creates a new booking. This configuration is frontend-only and is
centralized in `frontend/src/appConfig.js`. For example:

```sh
VITE_BOOKING_SAVE_MODE=edit mvn package
```

In edit mode, the frontend uses `PUT /booking/update`. When user management is
added, a user's preference can override this global frontend default.

The Maven build downloads a pinned Node.js/npm into `target/node/`, installs
the locked frontend dependencies with `npm ci`, and builds the React app during
`generate-resources`. You do not need to install Node.js or npm to package or
run the production Spring Boot application:

```sh
mvn package
```

To run Spring Boot directly from Maven and rebuild the frontend first:

```sh
mvn generate-resources spring-boot:run
```

For Vite hot-reload development, install Node.js/npm and run `npm ci` followed
by `npm run dev` from `frontend/`, with Spring Boot running on port 8080.
The build writes the bundled React app to the ignored
`src/main/resources/public/react-assets/` directory. `/rooms.html` and
`/cars.html` remain as redirects to the shared screen.
