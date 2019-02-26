# Test suite MantisBT (version 1.2.0)

### Run the application

Execute the Bash script to initialize the Docker image containing the web application:

`./run-docker.sh`

Inside the container, start the Apache server with PHP and MySQL:

`./run-services-docker.sh`

The application shall run at the address:

`http://localhost:3000/mantisbt/`

### Admin Credentials
username: `administrator`

password: `root`
