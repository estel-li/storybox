package service

import (
	"errors"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

type AuthService struct {
	adminPassword string
	jwtSecret     []byte
}

func NewAuthService(adminPassword, jwtSecret string) *AuthService {
	return &AuthService{adminPassword: adminPassword, jwtSecret: []byte(jwtSecret)}
}

func (s *AuthService) Login(password string) (string, error) {
	if password != s.adminPassword {
		return "", errors.New("invalid password")
	}
	claims := jwt.MapClaims{
		"role": "admin",
		"exp":  time.Now().Add(24 * time.Hour).Unix(),
		"iat":  time.Now().Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(s.jwtSecret)
}

func (s *AuthService) Validate(tokenString string) error {
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (any, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return s.jwtSecret, nil
	})
	if err != nil {
		return err
	}
	if !token.Valid {
		return errors.New("invalid token")
	}
	return nil
}
