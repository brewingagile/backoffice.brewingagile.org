# backoffice.brewingagile.org

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/brewingagile/backoffice.brewingagile.org?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Database

`docker run -p 5432:5432 postgres:9`

    CREATE DATABASE brewingagile;
    \c brewingagile
    CREATE EXTENSION "uuid-ossp";

Edit the `backend.conf`:

- username
- password

Default from Docker container is `postgresql` and empty password.
