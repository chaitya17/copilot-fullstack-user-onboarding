# Database Initialization Scripts

This directory contains database migration scripts that work with both MSSQL and Oracle databases.

## Structure

- `V1__init.sql` - Initial schema creation with user management and onboarding tables
- Future migrations will follow Flyway naming convention: `V{version}__{description}.sql`

## Database Differences

### MSSQL (Default)
- Uses `IDENTITY(1,1)` for auto-increment
- Uses `NVARCHAR` for Unicode strings
- Uses `DATETIME2` for timestamps
- Uses `BIT` for boolean values
- Uses `NVARCHAR(MAX)` for JSON storage

### Oracle
- Uses `GENERATED ALWAYS AS IDENTITY` for auto-increment
- Uses `VARCHAR2` for strings
- Uses `TIMESTAMP` for timestamps
- Uses `NUMBER(1) CHECK (col IN (0,1))` for boolean values
- Uses `CLOB` for JSON storage

## Schema Overview

### Tables Created

1. **users** - Core user information
   - Basic profile data (name, email, phone)
   - Authentication data (username, password hash)
   - Status flags (active, email verified)
   - Audit timestamps

2. **roles** - Role-based access control
   - Role definitions (USER, ADMIN, MODERATOR)
   - Role descriptions

3. **user_roles** - Many-to-many relationship
   - Links users to their assigned roles
   - Assignment timestamps

4. **onboarding_steps** - User onboarding progress
   - Tracks completion of onboarding steps
   - Stores step-specific data as JSON
   - Maintains step ordering

### Indexes Created
- Performance indexes on frequently queried columns
- Composite indexes for user onboarding queries

## Usage

### With Flyway (Recommended)
```bash
# Run migrations automatically on application startup
# Configured in Spring Boot application.yml
```

### Manual Execution

#### MSSQL
```bash
sqlcmd -S localhost -U sa -P YourPassword -d useronboard -i V1__init.sql
```

#### Oracle
```bash
sqlplus system/password@//localhost:1521/XEPDB1 @V1__init.sql
```

## Migration Strategy

### Development
- Use Flyway for automatic migrations
- Test migrations on both database types
- Include rollback scripts when necessary

### Production
- Always backup before migrations
- Test migrations on staging environment first
- Consider maintenance windows for large schema changes

## Database-Specific Considerations

### MSSQL
- Ensure SQL Server compatibility level is appropriate
- Consider using Azure SQL Database in production
- Enable encryption in transit for cloud deployments

### Oracle
- Ensure proper Oracle license compliance
- Consider using Oracle Autonomous Database in production
- Configure appropriate tablespace allocation

## Future Migrations

When adding new migrations:

1. **Follow naming convention**: `V{version}__{description}.sql`
2. **Include both database variants** in comments or separate files
3. **Test on both database types** before deployment
4. **Add appropriate indexes** for new columns
5. **Consider backward compatibility** for rolling deployments

## Troubleshooting

### Common Issues

#### MSSQL
```sql
-- Check if database exists
SELECT name FROM sys.databases WHERE name = 'useronboard';

-- Check table creation
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo';
```

#### Oracle
```sql
-- Check if user/schema exists
SELECT username FROM all_users WHERE username = 'USERONBOARD';

-- Check table creation
SELECT table_name FROM user_tables;
```

### Performance Issues
- Ensure indexes are created properly
- Check execution plans for slow queries
- Consider partitioning for large tables

### Connection Issues
- Verify JDBC URL format for your database type
- Check firewall and network connectivity
- Validate credentials and permissions
