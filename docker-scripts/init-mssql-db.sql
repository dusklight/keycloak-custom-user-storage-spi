-- Initialization script for the MS SQL Server:
-- * Creates an empty Keycloak database to be used by Keycloak container (schema will be added by Keycloak)
-- * Creates a demo database containing a users table and two sample users.

USE [master]

IF DB_ID (N'Keycloak') IS NULL
CREATE DATABASE Keycloak
GO

IF DB_ID (N'Dusklight') IS NULL
CREATE DATABASE Dusklight
GO

USE [Dusklight]
GO

DROP TABLE IF EXISTS [dbo].[Users]

CREATE TABLE [dbo].[Users](
	[UserId] [int] IDENTITY(1,1) NOT NULL,
	[Username] [nvarchar](100) NOT NULL,
	[PasswordHash] [nvarchar](128) NOT NULL,
	[FirstName] [nvarchar](100) NOT NULL,
	[LastName] [nvarchar](100) NOT NULL,
	[Department] [nvarchar](100) NOT NULL,

	CONSTRAINT AK_Username UNIQUE([Username])
) ON [PRIMARY]
GO

INSERT INTO [dbo].[Users]
	([Username], [PasswordHash], [FirstName], [LastName], [Department])
VALUES
	('alice', 'rhQPY84Iq//VX/uvKTFkBct+h+sLyKT9xU3Na4UY8MP1Qnqc+mxFKTmG+/7WvPzK94az6apHR2avgVbHxpfFuw==.kqUeLzJf4HUMSlS3B42Y0Q==.27500',
	 'Alice', 'Foo', 'Marketing')

INSERT INTO [dbo].[Users]
	([Username], [PasswordHash], [FirstName], [LastName], [Department])
VALUES
	('bob', 'R3yH86uwA+CoRFlUYN8gR7pNoN6pDwqFx6YJc0yRiJfr38kOxeMx2COTy1WsDyjeq98dVRFH8kVL9kkMmHq8Dg==.dPQVscuFLmRk8ECKsn0olg==.27500',
	 'Bob', 'Bar', 'Accounting')
GO
