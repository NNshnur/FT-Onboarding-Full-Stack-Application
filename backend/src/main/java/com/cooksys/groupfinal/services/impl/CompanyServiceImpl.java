package com.cooksys.groupfinal.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.cooksys.groupfinal.dtos.*;
import com.cooksys.groupfinal.entities.*;
import com.cooksys.groupfinal.exceptions.BadRequestException;
import com.cooksys.groupfinal.exceptions.NotAuthorizedException;
import com.cooksys.groupfinal.mappers.*;
import com.cooksys.groupfinal.repositories.UserRepository;
import org.springframework.stereotype.Service;

import com.cooksys.groupfinal.exceptions.NotFoundException;
import com.cooksys.groupfinal.repositories.CompanyRepository;
import com.cooksys.groupfinal.repositories.TeamRepository;
import com.cooksys.groupfinal.services.CompanyService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

	private final CompanyRepository companyRepository;
	private final TeamRepository teamRepository;
	private final FullUserMapper fullUserMapper;
	private final BasicUserMapper basicUserMapper;
	private final AnnouncementMapper announcementMapper;
	private final TeamMapper teamMapper;
	private final ProjectMapper projectMapper;
	private final ProfileMapper profileMapper;
	private final UserRepository userRepository;


	private Company findCompany(Long id) {
		Optional<Company> company = companyRepository.findById(id);
		if (company.isEmpty()) {
			throw new NotFoundException("A company with the provided id does not exist.");
		}
		return company.get();
	}

	private Team findTeam(Long id) {
		Optional<Team> team = teamRepository.findById(id);
		if (team.isEmpty()) {
			throw new NotFoundException("A team with the provided id does not exist.");
		}
		return team.get();
	}

	@Override
	public Set<FullUserDto> getAllUsers(Long id) {
		Company company = findCompany(id);
		Set<User> filteredUsers = new HashSet<>();
		company.getEmployees().forEach(filteredUsers::add);
		filteredUsers.removeIf(user -> !user.isActive());
		return fullUserMapper.entitiesToFullUserDtos(filteredUsers);
	}

	@Override
	public Set<AnnouncementDto> getAllAnnouncements(Long id) {
		Company company = findCompany(id);
		List<Announcement> sortedList = new ArrayList<Announcement>(company.getAnnouncements());
		sortedList.sort(Comparator.comparing(Announcement::getDate).reversed());
		Set<Announcement> sortedSet = new HashSet<Announcement>(sortedList);
		return announcementMapper.entitiesToDtos(sortedSet);
	}

	@Override
	public Set<TeamDto> getAllTeams(Long id) {
		Company company = findCompany(id);
		return teamMapper.entitiesToDtos(company.getTeams());
	}

	@Override
	public Set<ProjectDto> getAllProjects(Long companyId, Long teamId) {
		Company company = findCompany(companyId);
		Team team = findTeam(teamId);
		if (!company.getTeams().contains(team)) {
			throw new NotFoundException("A team with id " + teamId + " does not exist at company with id " + companyId + ".");
		}
		Set<Project> filteredProjects = new HashSet<>();
		team.getProjects().forEach(filteredProjects::add);
		filteredProjects.removeIf(project -> !project.isActive());
		return projectMapper.entitiesToDtos(filteredProjects);
	}


	private ProfileDto getProfileDtoForUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));

		return profileMapper.entityToDto(user.getProfile());
	}

	@Override
	public TeamDto addTeamToCompany(Long id, TeamDto teamDto) {
		if (teamDto == null || teamDto.getName() == null || teamDto.getDescription() == null) {
			throw new BadRequestException("Invalid information given.");
		}
		Company company = companyRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Company with id " + id + " was not found."));
		Team newTeam = new Team();
		newTeam.setName(teamDto.getName());
		newTeam.setDescription(teamDto.getDescription());
		if (teamDto.getTeammates() != null && !teamDto.getTeammates().isEmpty()) {
			Set<User> teammates = basicUserMapper.basicDtosToEntities(teamDto.getTeammates());
			for (User teammate : teammates) {
				ProfileDto profileDto = getProfileDtoForUser(teammate.getId());
				if (profileDto != null) {
					Profile profile = profileMapper.dtoToEntity(profileDto);
					teammate.setProfile(profile);
				}
			}
			newTeam.setTeammates(teammates);
		} else {
			newTeam.setTeammates(new HashSet<>());
		}
		Team savedTeam = teamRepository.saveAndFlush(newTeam);
		company.getTeams().add(savedTeam);
		companyRepository.saveAndFlush(company);
		return teamMapper.entityToDto(savedTeam);
	}
}




