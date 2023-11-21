import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { User } from '../models/user-model';
import { Announcement } from '../models/announcement-model';

@Injectable({
  providedIn: 'root'
})
export class ApiCallsService {
  private apiUrl = 'http://localhost:8080'

  constructor(private http: HttpClient) {}

  testUser1: User = {
    id: 1,
    username: "TestUser",
    firstName: "Test",
    lastName: "User",
    email: "test@user.com",
    active: true,
    admin: true, //Change this value to change login page for User
    status: "Joined",
    companies: [],
    teams: []
  }

  /**
   * Log-In API Call
   * Returns promise while it waits on the confirmation from the BE
   */
  async login(username: string, password: string) { /** make backend api call for the user */
    let requestUrl: string = this.apiUrl + "/users/login"
    let requestBody: Object = { username, password}


    return this.http.post<any>(requestUrl, requestBody).pipe(
      map(response => {
        //Create user Object based on response data
        const authenticatingUser: User = {
          id: response.id,
          username: username,
          firstName: response.profile.firstName,
          lastName: response.profile.lastName,
          email: response.profile.email,
          active: response.active,
          admin: response.admin,
          status: response.status,
          companies: response.companies,
          teams: response.teams
        };

        // Make an Array of User Team Id's
        let userTeams: String[] = response.teams.map((team:any) => {
          return team.id
        });

        // Use Session Storage for User object, team id's, and company id
        sessionStorage.setItem("user", JSON.stringify(authenticatingUser))
        sessionStorage.setItem("userTeams", JSON.stringify(userTeams))
        sessionStorage.setItem("userCompany", response.companies[0].id)

        return authenticatingUser;
      }),
      catchError(error => {
        if (error.status === 404) {
          // Handle the "user not found" error here, e.g., show a message to the user
          console.error('User not found:', error);
        } else {
          // Handle other errors, such as network issues or server errors
          console.error('Login error:', error);
        }

        // Rethrow the error or return a default value as needed
        return throwError(error);
      }))
  }

  async getCompanyUsers(companyId: number) {
    let requestUrl: string = this.apiUrl + "/companies/" + companyId + "/users"

    return this.http.get<any>(requestUrl).pipe(
      map(response => {
        console.log("Response:", response);
        let users = response

        return users;
      }),
      catchError(error => {
        if (error.status === 404) {
          // Handle the "user not found" error here, e.g., show a message to the user
          console.error('Users not found:', error);
        }
        // Rethrow the error or return a default value as needed
        return throwError(error);
      }))
  }

  async getAnnouncements(id: string) {
    let requestUrl: string = this.apiUrl + `/companies/${id}/announcements`
    return this.http.get<Announcement[]>(requestUrl);
  }

  async createAnnouncement(id: number, requestBody: Object) {
    let requestUrl: string = this.apiUrl + `/companies/${id}/announcements`
    return this.http.post<Announcement>(requestUrl, requestBody);
  }
}
