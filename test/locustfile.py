from locust import HttpLocust, TaskSet, task

class UserBehavior(TaskSet):
    @task
    def building(self):
        self.client.get("/building")
     
    @task
    def detail(self):
        self.client.get("/building/E")

class WebsiteUser(HttpLocust):
    task_set = UserBehavior
    min_wait=2000
    max_wait=9000
